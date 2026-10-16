/*
 * Copyright (c) 2020-2026 dzikoysk
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
package com.reposilite.auth.infrastructure

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.OidcService
import com.reposilite.auth.api.SessionDetails
import com.reposilite.shared.badRequest
import com.reposilite.token.RoutePermission
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenResponse
import com.reposilite.token.api.CreateAccessTokenWithNoNameRequest
import io.javalin.http.bodyAsClass
import io.javalin.openapi.OpenApiRequestBody
import panda.std.Result.supplyThrowing
import com.reposilite.shared.ErrorResponse
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse

internal class AuthenticationEndpoint(
    private val authenticationFacade: AuthenticationFacade,
    private val oidcService: OidcService,
) : ReposiliteRoutes() {

    @OpenApi(
        path = "/api/auth/me",
        methods = [HttpMethod.GET],
        summary = "Get token details",
        description = "Returns details about the requested token",
        tags = [ "Auth" ],
        headers = [ OpenApiParam(name = "Authorization", description = "Name and secret provided as basic auth credentials", required = true) ],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Details about the token for succeeded authentication",
                content = [ OpenApiContent(from = SessionDetails::class) ]
            ),
            OpenApiResponse(
                status = "401",
                description = "Error message related to the unauthorized access in case of any failure",
                content = [ OpenApiContent(from = ErrorResponse::class) ]
            )
        ]
    )
    private val authInfo = ReposiliteRoute<SessionDetails>("/api/auth/me", Route.GET) {
        response = authentication().flatMap {
            authenticationFacade.getSessionDetails(it.identifier)
        }
    }

    @OpenApi(
        path = "/api/auth/me/tokens",
        methods = [HttpMethod.GET],
        summary = "List own tokens",
        description = "Returns tokens created by the authenticated user",
        tags = [ "Auth" ],
        responses = [ OpenApiResponse(status = "200", content = [ OpenApiContent(from = Array<AccessTokenDto>::class) ]) ]
    )
    private val listUserTokens = ReposiliteRoute<List<AccessTokenDto>>("/api/auth/me/tokens", Route.GET) {
        authenticated {
            response = oidcService.getUserTokens(this)
        }
    }

    @OpenApi(
        path = "/api/auth/me/tokens",
        methods = [HttpMethod.POST],
        summary = "Create own token",
        description = "Creates a token named '<user>#<label>' with routes limited to the routes of the authenticated user, the secret is returned only once",
        tags = [ "Auth" ],
        requestBody = OpenApiRequestBody(content = [ OpenApiContent(from = CreateUserTokenRequest::class) ], required = true),
        responses = [ OpenApiResponse(status = "200", content = [ OpenApiContent(from = CreateAccessTokenResponse::class) ]) ]
    )
    private val createUserToken = ReposiliteRoute<CreateAccessTokenResponse>("/api/auth/me/tokens", Route.POST) {
        authenticated {
            response = supplyThrowing { ctx.bodyAsClass<CreateUserTokenRequest>() }
                .mapErr { badRequest("Failed to read body") }
                .filter({ request -> request.routes.all { route -> route.permissions.all { RoutePermission.findRoutePermissionByShortcut(it).isOk } } }) {
                    badRequest("Unknown route permission, available: r, w")
                }
                .flatMap { request ->
                    val routes = request.routes
                        .map { route -> CreateAccessTokenRequest.Route(route.path, route.permissions.map { RoutePermission.findRoutePermissionByShortcut(it).get() }.toSet()) }
                        .toSet()
                    oidcService.createUserToken(this, request.label, routes)
                }
        }
    }

    @OpenApi(
        path = "/api/auth/me/tokens/{label}",
        methods = [HttpMethod.DELETE],
        summary = "Delete own token",
        tags = [ "Auth" ],
        pathParams = [ OpenApiParam(name = "label", description = "Label of the token", required = true) ],
    )
    private val deleteUserToken = ReposiliteRoute<Unit>("/api/auth/me/tokens/{label}", Route.DELETE) {
        authenticated {
            response = oidcService.deleteUserToken(this, requireParameter("label"))
        }
    }

    override val routes = routes(authInfo, listUserTokens, createUserToken, deleteUserToken)

}

data class CreateUserTokenRequest(
    val label: String,
    val routes: List<CreateAccessTokenWithNoNameRequest.Route> = emptyList(),
)
