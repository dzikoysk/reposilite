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

@file:Suppress("FunctionName")

package com.reposilite.auth

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.ReposiliteSpecification
import com.reposilite.auth.application.AuthenticationSettings
import com.reposilite.auth.application.OidcSettings
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission.READ
import com.sun.net.httpserver.HttpServer
import kong.unirest.core.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.Base64

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class OidcIntegrationTest : ReposiliteSpecification() {

    private val http = HttpClient.newHttpClient() // does not follow redirects
    private var identityProvider: HttpServer? = null

    @AfterEach
    fun stopIdentityProvider() {
        identityProvider?.stop(0)
    }

    private fun useIdentityProvider(userinfo: String, tokens: String = """{ "access_token": "access-token" }"""): String {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val url = "http://127.0.0.1:${server.address.port}"
        fun respond(path: String, body: String) = server.createContext(path) { exchange ->
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.length.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        respond("/.well-known/openid-configuration", """{ "authorization_endpoint": "$url/authorize", "token_endpoint": "$url/token", "userinfo_endpoint": "$url/userinfo", "end_session_endpoint": "$url/logout" }""")
        respond("/token", tokens)
        respond("/userinfo", userinfo)
        server.start()
        identityProvider = server
        return url
    }

    @Test
    fun `should sign in with oidc and use self-service token for maven`() {
        // given: OIDC configured against a fake identity provider and a user with read access to the private repository
        val idp = useIdentityProvider("""{ "email": "john@company.com", "roles": ["reposilite-user"] }""")
        useFacade<SharedConfigurationFacade>().getDomainSettings<AuthenticationSettings>()
            .update(AuthenticationSettings(oidc = OidcSettings(enabled = true, discoveryUrl = idp, clientId = "reposilite", redirectUri = "$base/oidc/callback")))
        val (_, user) = useAuth("john@company.com", "unused-secret", routes = mapOf("/private" to READ)).let { it to useFacade<AccessTokenFacade>().getAccessToken(it.first)!! }
        val (repository, gav, file, content) = useDocument("private", "com/company/app/1.0", "app-1.0.jar", "jar-content", store = true)

        // when: the browser starts the login
        val login = http.send(HttpRequest.newBuilder(URI("$base/oidc/login")).build(), BodyHandlers.discarding())
        val location = login.headers().firstValue("Location").get()
        val cookie = login.headers().firstValue("Set-Cookie").get().substringBefore(';')
        assertThat(location).startsWith("$idp/authorize?").contains("code_challenge_method=S256")

        // and: the identity provider redirects back with code and state
        val state = location.substringAfter("state=").substringBefore('&')
        val callback = http.send(
            HttpRequest.newBuilder(URI("$base/oidc/callback?code=code&state=$state")).header("Cookie", cookie).build(),
            BodyHandlers.ofString()
        )

        // then: the page stores a web interface session for the user
        assertThat(callback.statusCode()).isEqualTo(200)
        val session = Regex("""'token-secret', "([^"]+)"""").find(callback.body())!!.groupValues[1]
        assertThat(Unirest.get("$base/api/auth/me").basicAuth("john@company.com", session).asString().status).isEqualTo(200)

        // and: logout ends the session at the identity provider and returns to Reposilite
        val logout = http.send(HttpRequest.newBuilder(URI("$base/oidc/logout")).build(), BodyHandlers.discarding())
        assertThat(logout.headers().firstValue("Location").get())
            .isEqualTo("$idp/logout?client_id=reposilite&post_logout_redirect_uri=" + URLEncoder.encode("$base/", Charsets.UTF_8))

        // and: a forged state is rejected
        val forged = http.send(HttpRequest.newBuilder(URI("$base/oidc/callback?code=code&state=forged")).header("Cookie", cookie).build(), BodyHandlers.discarding())
        assertThat(forged.statusCode()).isEqualTo(400)

        // when: the user creates a token for maven
        val created = Unirest.post("$base/api/auth/me/tokens")
            .basicAuth("john@company.com", session)
            .contentType("application/json")
            .body("""{ "label": "laptop", "routes": [ { "path": "/private", "permissions": ["r"] } ] }""")
            .asJson()
        assertThat(created.status).isEqualTo(200)
        val secret = created.body.`object`.getString("secret")

        // then: maven can download artifacts with it
        val artifact = Unirest.get("$base/$repository/$gav/$file").basicAuth("john@company.com#laptop", secret).asString()
        assertThat(artifact.status).isEqualTo(200)
        assertThat(artifact.body).isEqualTo(content)

        // and: tokens can't exceed the user's paths
        val exceeding = Unirest.post("$base/api/auth/me/tokens")
            .basicAuth("john@company.com", session)
            .contentType("application/json")
            .body("""{ "label": "ci", "routes": [ { "path": "/private", "permissions": ["r", "w"] } ] }""")
            .asString()
        assertThat(exceeding.status).isEqualTo(403)

        // when: a manager removes the path from the user
        useFacade<AccessTokenFacade>().deleteRoute(user.identifier, Route("/private", READ))

        // then: the user's token loses access immediately
        assertThat(Unirest.get("$base/$repository/$gav/$file").basicAuth("john@company.com#laptop", secret).asString().status).isEqualTo(403)
    }

    @Test
    fun `should grant manager permission from oidc role`() {
        // roles only in the id token, like FusionAuth with strict scope handling
        val idToken = listOf("""{"alg":"none"}""", """{"roles":["reposilite-manager"]}""", "signature")
            .joinToString(".") { Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray()) }
        val idp = useIdentityProvider("""{ "email": "boss@company.com" }""", """{ "access_token": "access-token", "id_token": "$idToken" }""")
        useFacade<SharedConfigurationFacade>().getDomainSettings<AuthenticationSettings>()
            .update(AuthenticationSettings(oidc = OidcSettings(enabled = true, discoveryUrl = idp, clientId = "reposilite", redirectUri = "$base/oidc/callback")))

        // signing in twice must not insert the manager permission twice (unique in the database)
        repeat(2) {
            val login = http.send(HttpRequest.newBuilder(URI("$base/oidc/login")).build(), BodyHandlers.discarding())
            val state = login.headers().firstValue("Location").get().substringAfter("state=").substringBefore('&')
            val cookie = login.headers().firstValue("Set-Cookie").get().substringBefore(';')
            val callback = http.send(HttpRequest.newBuilder(URI("$base/oidc/callback?code=code&state=$state")).header("Cookie", cookie).build(), BodyHandlers.ofString())
            assertThat(callback.statusCode()).isEqualTo(200)
            val session = Regex("""'token-secret', "([^"]+)"""").find(callback.body())!!.groupValues[1]

            assertThat(Unirest.get("$base/api/tokens").basicAuth("boss@company.com", session).asString().status).isEqualTo(200)
        }
    }

}
