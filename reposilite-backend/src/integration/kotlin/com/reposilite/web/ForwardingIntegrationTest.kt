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

package com.reposilite.web

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.ReposiliteSpecification
import com.reposilite.auth.application.AuthenticationSettings
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.web.application.WebSettings
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.UNAUTHORIZED
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit.SECONDS
import kong.unirest.core.Unirest.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private const val FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix"
private const val BASE_PATH = "/maven"
private const val FORWARDED_IP_HEADER = "X-Forwarded-For"

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class ForwardingIntegrationTest {

    @Nested
    inner class ForwardedPrefix : ReposiliteSpecification() {

        override fun overrideLocalConfiguration(localConfiguration: LocalConfiguration) {
            localConfiguration.basePath.update { BASE_PATH }
        }

        override fun overrideSharedConfiguration(sharedConfigurationFacade: SharedConfigurationFacade) {
            sharedConfigurationFacade.getDomainSettings<WebSettings>().update { it.copy(forwardedPrefix = FORWARDED_PREFIX_HEADER) }
        }

        @BeforeEach
        fun setupRepository() {
            useDocument("releases", "gav", "artifact.jar", "content", store = true)
        }

        @Test
        fun `should render index links under the configured base path for any forwarded request`() {
            // when: the directory is requested through the proxy with a header value that differs from the base path
            val response = get("$base/releases/gav")
                .header(FORWARDED_PREFIX_HEADER, "/ignored-value")
                .asString()

            // then: the configured base path is used regardless of the header value
            assertThat(response.isSuccess).isTrue
            assertThat(response.body).contains("<base href='/maven/releases/gav/'>")
        }

        @Test
        fun `should render index links at the root when the request is not forwarded`() {
            // when: the directory is requested directly, without the forwarded header
            val response = get("$base/releases/gav").asString()

            // then: links fall back to the root
            assertThat(response.isSuccess).isTrue
            assertThat(response.body).contains("<base href='/releases/gav/'>")
        }

        @Test
        fun `should render the 404 dashboard link under the configured base path when forwarded`() {
            // when: a missing resource is requested through the proxy
            val response = get("$base/releases/unknown-gav/unknown-file")
                .header(FORWARDED_PREFIX_HEADER, "/ignored-value")
                .asString()

            // then: the dashboard link uses the configured base path
            assertThat(response.status).isEqualTo(NOT_FOUND.code)
            assertThat(response.body).contains("/maven/#")
        }

        @Test
        fun `should render the 404 dashboard link at the root when the request is not forwarded`() {
            // when: a missing resource is requested directly, without the forwarded header
            val response = get("$base/releases/unknown-gav/unknown-file").asString()

            // then: the dashboard link falls back to the root
            assertThat(response.status).isEqualTo(NOT_FOUND.code)
            assertThat(response.body).contains("href=\"/#")
            assertThat(response.body).doesNotContain("/maven/#")
        }

    }

    @Nested
    inner class DisabledForwardedPrefix : ReposiliteSpecification() {

        override fun overrideLocalConfiguration(localConfiguration: LocalConfiguration) {
            localConfiguration.basePath.update { BASE_PATH }
        }

        @BeforeEach
        fun setupRepository() {
            useDocument("releases", "gav", "artifact.jar", "content", store = true)
        }

        @Test
        fun `should always use the configured base path when the feature is disabled`() {
            // when: a request arrives without the forwarded header
            val direct = get("$base/releases/gav").asString()

            // then: the base path stays configured, not the root
            assertThat(direct.body).contains("<base href='/maven/releases/gav/'>")

            // when: a request arrives carrying the forwarded header
            val forwarded = get("$base/releases/gav").header(FORWARDED_PREFIX_HEADER, "/other").asString()

            // then: the header is ignored and the base path stays configured
            assertThat(forwarded.body).contains("<base href='/maven/releases/gav/'>")
        }

    }

    @Nested
    inner class ForwardedIp : ReposiliteSpecification() {

        private val manager = useDefaultManagementToken()

        override fun overrideSharedConfiguration(sharedConfigurationFacade: SharedConfigurationFacade) {
            sharedConfigurationFacade.getDomainSettings<AuthenticationSettings>().update {
                it.copy(bruteForceProtection = it.bruteForceProtection.copy(enabled = true, maxAttempts = 2))
            }
        }

        @Test
        fun `should share the socket IP ban between HTTP and WebSocket by default`() {
            // given: a direct installation that accepts the manager credentials
            assertThat(authenticate(manager.second)).isEqualTo(OK.code)

            // when: failed logins arrive over both transports with different forwarded addresses
            failWebSocketAuthentication(FORWARDED_IP_HEADER)
            assertThat(authenticate("wrong-secret", "192.0.2.2")).isEqualTo(UNAUTHORIZED.code)

            // then: the socket IP is blocked even when the next request supplies another forwarded address
            assertThat(authenticate(manager.second, "192.0.2.3")).isEqualTo(UNAUTHORIZED.code)
        }

        @Test
        fun `should share a forwarded IP ban across transports regardless of header casing`() {
            // given: an installation configured to trust the proxy header
            val proxyHeader = "X-Reposilite-Client-IP"
            useFacade<SharedConfigurationFacade>().getDomainSettings<WebSettings>().update {
                it.copy(forwardedIp = proxyHeader)
            }

            // when: one client fails authentication over both transports
            failWebSocketAuthentication(proxyHeader.lowercase())
            assertThat(authenticate("wrong-secret", "192.0.2.1", proxyHeader)).isEqualTo(UNAUTHORIZED.code)

            // then: both attempts count against that client only
            assertThat(authenticate(manager.second, "192.0.2.1", proxyHeader)).isEqualTo(UNAUTHORIZED.code)
            assertThat(authenticate(manager.second, "192.0.2.2", proxyHeader)).isEqualTo(OK.code)
        }

        @Test
        fun `should count failed logins against the address appended by the proxy`() {
            // given: an installation configured to trust X-Forwarded-For
            useFacade<SharedConfigurationFacade>().getDomainSettings<WebSettings>().update {
                it.copy(forwardedIp = FORWARDED_IP_HEADER)
            }

            // when: one client sends failed logins with different prefixes
            assertThat(authenticate("wrong-secret", "192.0.2.2, 192.0.2.1")).isEqualTo(UNAUTHORIZED.code)
            assertThat(authenticate("wrong-secret", "192.0.2.3, 192.0.2.1")).isEqualTo(UNAUTHORIZED.code)

            // then: the appended client is banned and other clients can still authenticate
            assertThat(authenticate(manager.second, "192.0.2.4, 192.0.2.1")).isEqualTo(UNAUTHORIZED.code)
            assertThat(authenticate(manager.second, "192.0.2.3")).isEqualTo(OK.code)
        }

        private fun authenticate(secret: String, forwarded: String? = null, headerName: String = FORWARDED_IP_HEADER): Int =
            get("$base/api/tokens/${manager.first}")
                .basicAuth(manager.first, secret)
                .apply { forwarded?.let { header(headerName, it) } }
                .asString()
                .status

        private fun failWebSocketAuthentication(headerName: String) {
            val response = CompletableFuture<String>()
            val listener = object : WebSocket.Listener {
                override fun onText(socket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                    response.complete(data.toString())
                    return null
                }
            }

            val endpoint = URI.create(base.replaceFirst("http://", "ws://") + "/api/console/sock")
            val socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .header(headerName, "192.0.2.1")
                .buildAsync(endpoint, listener)
                .get(5, SECONDS)

            try {
                socket.sendText("Authorization:${manager.first}:wrong-secret", true).get(5, SECONDS)
                assertThat(response.get(5, SECONDS)).contains("Invalid authorization credentials")
            } finally {
                socket.abort()
            }
        }

    }

}
