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

import com.reposilite.auth.api.OAuth2SessionRequest
import com.reposilite.auth.api.OAuth2SessionResponse
import com.reposilite.auth.application.OAuth2Settings
import com.reposilite.auth.oauth2.OAuth2Service
import com.reposilite.auth.oauth2.urlEncoded
import com.reposilite.frontend.FrontendFacade
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.extractFromHeader
import com.reposilite.shared.notFoundError
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.DELETE
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.Route.POST
import io.javalin.http.Header
import io.javalin.http.HttpStatus.NO_CONTENT
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse
import panda.std.asError
import panda.std.reactive.Reference

internal class OAuth2Endpoints(
    private val oauth2Service: OAuth2Service,
    private val oauth2Settings: Reference<OAuth2Settings>,
    private val frontendFacade: FrontendFacade
) : ReposiliteRoutes() {

    @OpenApi(
        path = "/api/auth/oauth2/login",
        methods = [HttpMethod.GET],
        summary = "Sign in with identity provider",
        description = "Redirects to the configured OAuth2 identity provider to start the sign in process",
        tags = [ "Auth" ],
        responses = [
            OpenApiResponse(status = "302", description = "Redirect to the identity provider"),
            OpenApiResponse(status = "404", description = "OAuth2 authentication is disabled", content = [ OpenApiContent(from = ErrorResponse::class) ]),
            OpenApiResponse(status = "502", description = "Identity provider is not available", content = [ OpenApiContent(from = ErrorResponse::class) ])
        ]
    )
    private val login = ReposiliteRoute<Any>("/api/auth/oauth2/login", GET) {
        whenEnabled {
            oauth2Service.startLogin(baseUrl())
                .peek { ctx.redirect(it) }
                .onError { response = it.asError() }
        }
    }

    @OpenApi(
        path = "/api/auth/oauth2/callback",
        methods = [HttpMethod.GET],
        summary = "Finish sign in with identity provider",
        description = "Redirect target for the identity provider. Redirects to the dashboard with one-time code (oauth2_code) or with error message (oauth2_error)",
        tags = [ "Auth" ],
        queryParams = [
            OpenApiParam(name = "state", description = "State generated for the sign in request"),
            OpenApiParam(name = "code", description = "Authorization code issued by the identity provider")
        ],
        responses = [
            OpenApiResponse(status = "302", description = "Redirect to the dashboard"),
            OpenApiResponse(status = "404", description = "OAuth2 authentication is disabled", content = [ OpenApiContent(from = ErrorResponse::class) ])
        ]
    )
    private val callback = ReposiliteRoute<Any>("/api/auth/oauth2/callback", GET) {
        whenEnabled {
            val dashboard = baseUrl() + "/"

            oauth2Service.completeLogin(state = queryParameter("state"), code = queryParameter("code"))
                .peek { ctx.redirect("$dashboard?oauth2_code=${it.urlEncoded()}") }
                .onError { ctx.redirect("$dashboard?oauth2_error=${it.message.urlEncoded()}") }
        }
    }

    @OpenApi(
        path = "/api/auth/oauth2/session",
        methods = [HttpMethod.POST],
        summary = "Open session",
        description = "Exchanges one-time code received after sign in for credentials of the session, that can be used like a name and secret of access token",
        tags = [ "Auth" ],
        requestBody = OpenApiRequestBody(content = [ OpenApiContent(from = OAuth2SessionRequest::class) ]),
        responses = [
            OpenApiResponse(status = "200", description = "Credentials of the session", content = [ OpenApiContent(from = OAuth2SessionResponse::class) ]),
            OpenApiResponse(status = "401", description = "Unknown or expired code", content = [ OpenApiContent(from = ErrorResponse::class) ]),
            OpenApiResponse(status = "404", description = "OAuth2 authentication is disabled", content = [ OpenApiContent(from = ErrorResponse::class) ])
        ]
    )
    private val openSession = ReposiliteRoute<OAuth2SessionResponse>("/api/auth/oauth2/session", POST) {
        whenEnabled {
            response = oauth2Service.redeemCode(body<OAuth2SessionRequest>().code)
        }
    }

    @OpenApi(
        path = "/api/auth/oauth2/session",
        methods = [HttpMethod.DELETE],
        summary = "Close session",
        description = "Invalidates session associated with provided credentials",
        tags = [ "Auth" ],
        headers = [ OpenApiParam(name = "Authorization", description = "Name and secret of the session provided as basic auth credentials", required = true) ],
        responses = [
            OpenApiResponse(status = "204", description = "Session is closed"),
            OpenApiResponse(status = "404", description = "OAuth2 authentication is disabled", content = [ OpenApiContent(from = ErrorResponse::class) ])
        ]
    )
    private val closeSession = ReposiliteRoute<Any>("/api/auth/oauth2/session", DELETE) {
        whenEnabled {
            extractFromHeader(ctx.header(Header.AUTHORIZATION)).peek { (_, secret) -> oauth2Service.logout(secret) }
            ctx.status(NO_CONTENT)
        }
    }

    override val routes = routes(login, callback, openSession, closeSession)

    private fun <R> ContextDsl<R>.whenEnabled(block: () -> Unit) =
        when {
            oauth2Service.isEnabled() -> block()
            else -> response = notFoundError("OAuth2 authentication is disabled")
        }

    /** Public URL of this instance without trailing slash */
    private fun ContextDsl<*>.baseUrl(): String =
        oauth2Settings.map { it.redirectBaseUrl }
            .ifBlank { "${ctx.scheme()}://${ctx.host()}${frontendFacade.resolveBasePath(ctx.header(frontendFacade.forwardedPrefixHeader.get()))}" }
            .trimEnd('/')

}
