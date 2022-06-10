/*
 * Copyright (c) 2022 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.token.infrastructure

import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenResponse
import com.reposilite.token.api.CreateAccessTokenWithNoNameRequest
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.notFoundError
import com.reposilite.web.routing.RouteMethod.DELETE
import com.reposilite.web.routing.RouteMethod.GET
import com.reposilite.web.routing.RouteMethod.PUT
import io.javalin.http.HttpCode
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import panda.std.Result.ok
import panda.std.asSuccess

internal class AccessTokenApiEndpoints(private val accessTokenFacade: AccessTokenFacade) : ReposiliteRoutes() {

    @OpenApi(
        path = "/api/tokens",
        tags = ["Tokens"],
        summary = "Returns all existing tokens and data such as their permissions. Note: Requires Manager",
        methods = [HttpMethod.GET]
    )
    val tokens = ReposiliteRoute<Collection<AccessTokenDto>>("/api/tokens", GET) {
        managerOnly {
            response = ok(accessTokenFacade.getAccessTokens())
        }
    }

    @OpenApi(
        path = "/api/tokens/{name}",
        tags = ["Tokens"],
        summary = "Returns data about the token given via it's name. Note: Requires manager or you must be the token owner",
        pathParams = [OpenApiParam(name = "name", description = "Name of the token to be deleted", required = true)],
        methods = [HttpMethod.GET]
    )
    val token = ReposiliteRoute<AccessTokenDto>("/api/tokens/{name}", GET) {
        authenticated {
            response = accessTokenFacade.getAccessToken(requireParameter("name"))
                ?.takeIf { isManager() || name == it.name }
                ?.asSuccess()
                ?: errorResponse(HttpCode.FORBIDDEN, "You must be the token owner or a manager to access this.")
        }
    }

    @OpenApi(
        path = "/api/tokens/{name}",
        tags = ["Tokens"],
        summary = "Creates / Updates a token via the specified body. Note: Requires manager permission.",
        requestBody = OpenApiRequestBody(
            content = [OpenApiContent(CreateAccessTokenWithNoNameRequest::class)],
            required = true,
            description = "Data about the account including the secret and it's permissions"
        ),
        pathParams = [OpenApiParam(name = "name", description = "Name of the token to be deleted", required = true)],
        methods = [HttpMethod.PUT]
    )
    val createOrUpdateToken = ReposiliteRoute<CreateAccessTokenResponse>("/api/tokens/{name}", PUT) {
        managerOnly {
            response = panda.std.Result.attempt { ctx.bodyAsClass<CreateAccessTokenWithNoNameRequest>() }
                .mapErr { ErrorResponse(HttpCode.BAD_REQUEST, "Failed to read body") }
                .map { request ->
                    Pair(
                        accessTokenFacade.createAccessToken(CreateAccessTokenRequest(request.type, requireParameter("name"), secret = request.secret)),
                        request.permissions.mapNotNull { AccessTokenPermission.findByAny(it) }
                    )
                }
                .peek { (token, permissions) ->
                    permissions.forEach { accessTokenFacade.addPermission(token.accessToken.identifier, it) }
                }
                .map { (response, _) -> response }
        }
    }

    @OpenApi(
        path = "/api/tokens/{name}",
        tags = ["Tokens"],
        summary = "Deletes the token specified via it's name. Note: Requires Manager",
        pathParams = [OpenApiParam(name = "name", description = "Name of the token to be deleted", required = true)],
        methods = [HttpMethod.DELETE]
    )
    val deleteToken = ReposiliteRoute<Unit>("/api/tokens/{name}", DELETE) {
        managerOnly {
            response = accessTokenFacade.getAccessToken(requireParameter("name"))
                ?.let { accessTokenFacade.deleteToken(it.identifier) }
                ?: notFoundError("Token not found")
        }
    }

    override val routes = routes(tokens, token, createOrUpdateToken, deleteToken)

}
