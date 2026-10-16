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

import com.fasterxml.jackson.databind.ObjectMapper
import com.reposilite.auth.OidcService
import com.reposilite.shared.notFoundError
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.GET
import java.security.MessageDigest

private const val LOGIN_COOKIE = "reposilite-oidc"

/** Browser endpoints of the OpenID Connect login, see [OidcService] */
internal class OidcEndpoints(private val oidcService: OidcService) : ReposiliteRoutes() {

    private val json = ObjectMapper()

    private val login = ReposiliteRoute<Unit>("/oidc/login", GET) {
        if (!oidcService.isEnabled()) return@ReposiliteRoute run { response = notFoundError("OIDC is disabled") }
        val request = oidcService.createLoginRequest()
        // no Path attribute: the cookie stays scoped to <base path>/oidc
        ctx.res().addHeader("Set-Cookie", "$LOGIN_COOKIE=${request.state}.${request.verifier}; Max-Age=300; HttpOnly; Secure; SameSite=Lax")
        ctx.redirect(request.url)
    }

    private val callback = ReposiliteRoute<Unit>("/oidc/callback", GET) {
        if (!oidcService.isEnabled()) return@ReposiliteRoute run { response = notFoundError("OIDC is disabled") }
        // a reload of the callback page after successful login has no cookie anymore, the session is already in localStorage.
        // never start a new login here: a cookie the browser refuses (e.g. 'Secure' over plain HTTP) would loop through the identity provider forever
        val (expectedState, verifier) = ctx.cookie(LOGIN_COOKIE)?.split('.')?.takeIf { it.size == 2 }
            ?: return@ReposiliteRoute ctx.redirect("../")
        ctx.res().addHeader("Set-Cookie", "$LOGIN_COOKIE=; Max-Age=0; HttpOnly; Secure; SameSite=Lax")
        ctx.header("Cache-Control", "no-store")

        val state = ctx.queryParam("state") ?: ""
        val code = ctx.queryParam("code")
        if (code == null || !MessageDigest.isEqual(state.toByteArray(), expectedState.toByteArray())) {
            ctx.status(400).html(page("Sign-in failed: ${ctx.queryParam("error_description") ?: ctx.queryParam("error") ?: "invalid state"}"))
            return@ReposiliteRoute
        }

        oidcService.login(code, verifier)
            .peek { session ->
                // same localStorage keys as the web interface (store/session.js), the secret never appears in an URL
                ctx.html(page("Signing in...", """
                    <script>
                      localStorage.setItem('token-name', ${js(session.name)})
                      localStorage.setItem('token-secret', ${js(session.secret)})
                      localStorage.setItem('token-oidc', 'true')
                      location.replace('../')
                    </script>
                """))
            }
            .onError { ctx.status(it.status).html(page(it.message)) }
    }

    private val logout = ReposiliteRoute<Unit>("/oidc/logout", GET) {
        if (!oidcService.isEnabled()) return@ReposiliteRoute run { response = notFoundError("OIDC is disabled") }
        ctx.redirect(oidcService.createLogoutUrl() ?: "../")
    }

    override val routes = routes(login, callback, logout)

    private fun js(value: String): String =
        json.writeValueAsString(value).replace("<", "\\u003c")

    private fun page(message: String, script: String = ""): String =
        """<!doctype html><html><head><meta charset="utf-8"><title>Reposilite</title></head>
           <body style="font-family:system-ui,sans-serif;text-align:center;margin-top:20vh">
           <p>${escape(message)}</p><p><a href="../">Back to Reposilite</a> · <a href="login">Sign in again</a> · <a href="logout">Sign out</a></p>$script</body></html>"""

    private fun escape(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

}

