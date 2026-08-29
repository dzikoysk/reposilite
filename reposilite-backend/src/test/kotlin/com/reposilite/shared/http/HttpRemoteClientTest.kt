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

package com.reposilite.shared.http

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.shared.http.AuthenticationMethod.BASIC
import com.reposilite.shared.http.AuthenticationMethod.CUSTOM_HEADER
import com.reposilite.storage.api.DocumentInfo
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import java.util.Base64

private val REMOTE_CONTENT = "reposilite".toByteArray()

class HttpRemoteClientTest {

    private lateinit var server: HttpServer
    private lateinit var client: RemoteClient

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.start()
        client = HttpRemoteClientProvider(InMemoryLogger()).defaultClient
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun `should reuse default client`() {
        val provider = HttpRemoteClientProvider(InMemoryLogger())

        assertThat(provider.defaultClient).isSameAs(provider.defaultClient)
    }

    @Test
    fun `should get remote content with basic authentication`() {
        val expectedAuthorization = "Basic " + Base64.getEncoder().encodeToString("user:password".toByteArray())
        server.createContext("/artifact.jar") { exchange ->
            assertThat(exchange.requestMethod).isEqualTo("GET")
            assertThat(exchange.requestHeaders.getFirst("User-Agent")).isEqualTo("Reposilite")
            assertThat(exchange.requestHeaders.getFirst("Authorization")).isEqualTo(expectedAuthorization)
            exchange.respond(200, REMOTE_CONTENT)
        }

        val credentials = credentials(BASIC, "user", "password")
        val content = client.get(url("/artifact.jar"), credentials, 3, 15).get()

        content.use { assertThat(it.readAllBytes()).isEqualTo(REMOTE_CONTENT) }
    }

    @Test
    fun `should support custom authentication header`() {
        server.createContext("/artifact.jar") { exchange ->
            assertThat(exchange.requestHeaders.getFirst("Private-Token")).isEqualTo("secret")
            exchange.respond(200, REMOTE_CONTENT)
        }

        val credentials = credentials(CUSTOM_HEADER, "Private-Token", "secret")
        val content = client.get(url("/artifact.jar"), credentials, 3, 15).get()

        content.use { assertThat(it.readAllBytes()).isEqualTo(REMOTE_CONTENT) }
    }

    @Test
    fun `should resolve remote file details`() {
        val lastModified = Instant.parse("2026-08-30T12:00:00Z")
        server.createContext("/artifact.jar") { exchange ->
            assertThat(exchange.requestMethod).isEqualTo("HEAD")
            exchange.responseHeaders.set("Content-Length", REMOTE_CONTENT.size.toString())
            exchange.responseHeaders.set("Content-Type", "application/java-archive")
            exchange.responseHeaders.set("Last-Modified", RFC_1123_DATE_TIME.format(lastModified.atZone(ZoneOffset.UTC)))
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }

        val details = client.head(url("/artifact.jar"), null, 3, 15).get() as DocumentInfo

        assertThat(details.name).isEqualTo("artifact.jar")
        assertThat(details.contentLength).isEqualTo(REMOTE_CONTENT.size.toLong())
        assertThat(details.contentType.toString()).isEqualTo("application/java-archive")
        assertThat(details.lastModifiedTime).isEqualTo(lastModified)
    }

    @Test
    fun `should preserve response validation`() {
        server.createContext("/missing") { exchange -> exchange.respond(404, ByteArray(0)) }
        server.createContext("/throttled") { exchange -> exchange.respond(429, ByteArray(0)) }
        server.createContext("/error") { exchange -> exchange.respond(500, ByteArray(0)) }
        server.createContext("/page") { exchange ->
            exchange.responseHeaders.set("Content-Type", "text/html")
            exchange.respond(200, REMOTE_CONTENT)
        }

        val missing = client.get(url("/missing"), null, 3, 15)
        val throttled = client.get(url("/throttled"), null, 3, 15)
        val error = client.get(url("/error"), null, 3, 15)
        val page = client.get(url("/page"), null, 3, 15)

        assertThat(missing.error.status).isEqualTo(404)
        assertThat(missing.error.message).isEqualTo("Unsuccessful request (404)")
        assertThat(throttled.error.status).isEqualTo(429)
        assertThat(throttled.error.message).isEqualTo("Unsuccessful request (429)")
        assertThat(error.error.status).isEqualTo(406)
        assertThat(error.error.message).isEqualTo("Unsuccessful request (500)")
        assertThat(page.error.status).isEqualTo(406)
        assertThat(page.error.message).isEqualTo("Illegal file type (text/html)")
    }

    @Test
    fun `should reject invalid URI`() {
        val result = client.get("not a valid URI", null, 3, 15)

        assertThat(result.error.status).isEqualTo(400)
        assertThat(result.error.message).isEqualTo("Invalid remote URI: not a valid URI")
    }

    private fun url(path: String): String =
        "http://${server.address.hostString}:${server.address.port}$path"

    private fun credentials(method: AuthenticationMethod, login: String, password: String): RemoteCredentials =
        object : RemoteCredentials {
            override val method = method
            override val login = login
            override val password = password
        }

    private fun HttpExchange.respond(status: Int, content: ByteArray) {
        sendResponseHeaders(status, content.size.toLong())
        responseBody.use { it.write(content) }
    }

}
