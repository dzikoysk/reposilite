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
package com.reposilite.auth.infrastructure

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.api.SessionDetails
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse

internal class AuthenticationEndpoint(private val authenticationFacade: AuthenticationFacade) : ReposiliteRoutes() {

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
    private val authInfo = ReposiliteRoute<SessionDetails>("/api/auth/me", GET) {
        response = authentication().flatMap {
            authenticationFacade.getSessionDetails(it.identifier)
        }
    }

    override val routes = routes(authInfo)

}
