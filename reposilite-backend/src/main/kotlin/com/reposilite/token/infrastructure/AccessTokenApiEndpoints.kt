/*
 * Copyright (c) 2021 dzikoysk
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
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenWithNoNameRequest
import com.reposilite.web.application.ReposiliteRoute
import com.reposilite.web.application.ReposiliteRoutes
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.unauthorizedError
import com.reposilite.web.routing.RouteMethod
import io.javalin.http.HttpCode
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody

internal class AccessTokenApiEndpoints(private val accessTokenFacade: AccessTokenFacade) : ReposiliteRoutes() {
    @OpenApi(
        tags = ["Access Tokens"],
        path = "/api/tokens",
        summary = "Returns all existing tokens and data such as their permissions. Note: Requires Manager",
        methods = [HttpMethod.GET]
    )
    val tokens = ReposiliteRoute("/api/tokens", RouteMethod.GET) {
        authenticated {
            response = if (isManager()) {
                accessTokenFacade.getTokensResponse()
            } else {
                unauthorizedError("You must be a manager to access this endpoint!")
            }
        }
    }

    @OpenApi(
        tags = ["Access Tokens"],
        path = "/api/tokens/{name}",
        summary = "Returns data about the token given via it's name. Note: Requires manager or you must be the token owner",
        pathParams = [OpenApiParam(name = "name", description = "Name of the token to be deleted", required = true)],
        methods = [HttpMethod.GET]
    )
    val token = ReposiliteRoute("/api/tokens/{name}", RouteMethod.GET) {
        authenticated {
            val tokenName = requireParameter("name")
            val token = accessTokenFacade.getToken(tokenName)
            response = if (isManager() || secret == token?.secret) {
                accessTokenFacade.getTokenResponse(tokenName)
            } else {
                unauthorizedError("You must be the token owner or a manager to access this!")
            }
        }
    }

    @OpenApi(
        tags = ["Access Tokens"],
        path = "/api/tokens/{name}",
        summary = "Creates / Updates a token via the specified body. Note: Requires Manager",
        requestBody = OpenApiRequestBody(
            content = [OpenApiContent(CreateAccessTokenWithNoNameRequest::class)],
            required = true,
            description = "Data about the account including the secret and it's permissions"
        ),
        pathParams = [OpenApiParam(name = "name", description = "Name of the token to be deleted", required = true)],
        methods = [HttpMethod.PUT]
    )
    val createOrUpdateToken = ReposiliteRoute("/api/tokens/{name}", RouteMethod.PUT) {
        authenticated {
            val body = runCatching { ctx.bodyAsClass<CreateAccessTokenWithNoNameRequest>() }.getOrNull()
            response = if (body != null) {
                if (!isManager()) {
                    errorResponse(HttpCode.UNAUTHORIZED, "You must be a manager to access this endpoint!")
                } else {
                    accessTokenFacade.createOrUpdateToken(
                        CreateAccessTokenRequest(
                            requireParameter("name"),
                            body.secret,
                            body.permissions.mapNotNull { AccessTokenPermission.findByAll(it) }.toSet()
                        )
                    )
                }
            } else {
                unauthorizedError("Failed to parse body, make sure you've followed the specification!")
            }
        }
    }

    @OpenApi(
        tags = ["Access Tokens"],
        path = "/api/tokens/{name}",
        summary = "Deletes the token specified via it's name. Note: Requires Manager",
        pathParams = [OpenApiParam(name = "name", description = "Name of the token to be deleted", required = true)],
        methods = [HttpMethod.DELETE]
    )
    val deleteToken = ReposiliteRoute("/api/tokens/{name}", RouteMethod.DELETE) {
        authenticated {
            response = if (isManager()) {
                accessTokenFacade.deleteTokenWithResponse(requireParameter("name"))
            } else {
                unauthorizedError("You must be a manager to access this endpoint!")
            }
        }
    }

    override val routes = setOf(tokens, token, createOrUpdateToken, deleteToken)
}