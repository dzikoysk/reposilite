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

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets.ISO_8859_1
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPOutputStream

private val CONTENT = "reposilite".toByteArray()
private const val USER_AGENT = "Reposilite/test"

class HttpClientTest {

    private lateinit var server: HttpServer
    private lateinit var client: HttpClient

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.start()
        client = HttpClient(USER_AGENT)
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun `should execute request`() {
        server.createContext("/artifact.jar") { exchange ->
            assertThat(exchange.requestMethod).isEqualTo("GET")
            assertThat(exchange.requestHeaders.getFirst("User-Agent")).isEqualTo(USER_AGENT)
            assertThat(exchange.requestHeaders.getFirst("Private-Token")).isEqualTo("secret")
            exchange.respond(200, CONTENT)
        }

        val response = client.execute(requestForPath("/artifact.jar", mapOf("Private-Token" to "secret")))

        assertThat(response.statusCode).isEqualTo(200)
        response.openBody().use { assertThat(it.readAllBytes()).isEqualTo(CONTENT) }
    }

    @Test
    fun `should decode gzip content`() {
        val compressed = ByteArrayOutputStream().use { output ->
            GZIPOutputStream(output).use { it.write(CONTENT) }
            output.toByteArray()
        }
        server.createContext("/artifact.jar") { exchange ->
            exchange.responseHeaders.set("Content-Encoding", "gzip")
            exchange.respond(200, compressed)
        }

        val response = client.execute(requestForPath("/artifact.jar"))

        response.openBody().use { assertThat(it.readAllBytes()).isEqualTo(CONTENT) }
    }

    @Test
    fun `should follow redirects without forwarding authorization`() {
        val redirectedAuthorization = AtomicReference<String?>()
        val redirectedPrivateToken = AtomicReference<String?>()
        server.createContext("/redirect") { exchange ->
            exchange.responseHeaders.set("Location", "/artifact.jar")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/artifact.jar") { exchange ->
            redirectedAuthorization.set(exchange.requestHeaders.getFirst("Authorization"))
            redirectedPrivateToken.set(exchange.requestHeaders.getFirst("Private-Token"))
            exchange.respond(200, CONTENT)
        }

        val response = client.execute(
            requestForPath(
                "/redirect",
                mapOf(
                    "Authorization" to "Basic credentials",
                    "Private-Token" to "secret",
                ),
            )
        )

        response.openBody().close()
        assertThat(redirectedAuthorization).hasValue(null)
        assertThat(redirectedPrivateToken).hasValue("secret")
    }

    @Test
    fun `should get content through socks proxy`() {
        Socks5TestServer().use { socks ->
            val proxy = Proxy(Proxy.Type.SOCKS, socks.address)
            val proxiedClient = HttpClient(USER_AGENT, proxy)
            val response = proxiedClient.execute(request("http://127.0.0.1:6553/artifact.jar"))

            response.openBody().use { assertThat(it.readAllBytes()).isEqualTo(CONTENT) }
            assertThat(socks.requestLine()).isEqualTo("GET /artifact.jar HTTP/1.1")
        }
    }

    private fun requestForPath(path: String, headers: Map<String, String> = emptyMap()): HttpRequest =
        request(url(path), headers)

    private fun request(url: String, headers: Map<String, String> = emptyMap()): HttpRequest =
        HttpRequest(
            method = "GET",
            url = URI(url).toURL(),
            headers = headers,
            connectTimeoutInMillis = 3000,
            readTimeoutInMillis = 15_000,
        )

    private fun url(path: String): String =
        "http://${server.address.hostString}:${server.address.port}$path"

    private fun HttpExchange.respond(status: Int, content: ByteArray) {
        sendResponseHeaders(status, content.size.toLong())
        responseBody.use { it.write(content) }
    }

}

private class Socks5TestServer : AutoCloseable {

    private val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    private val executor = Executors.newSingleThreadExecutor()
    private val request = executor.submit<String> {
        server.accept().use { socket -> handle(socket) }
    }

    val address: InetSocketAddress = server.localSocketAddress as InetSocketAddress

    fun requestLine(): String =
        request.get(5, SECONDS)

    private fun handle(socket: Socket): String {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()

        check(input.readRequired() == 5)
        repeat(input.readRequired()) { input.readRequired() }
        output.write(byteArrayOf(5, 0))
        output.flush()

        check(input.readRequired() == 5)
        check(input.readRequired() == 1)
        input.readRequired()
        when (input.readRequired()) {
            1 -> input.readRequired(4)
            3 -> input.readRequired(input.readRequired())
            4 -> input.readRequired(16)
            else -> error("Unsupported SOCKS address type")
        }
        input.readRequired(2)
        output.write(byteArrayOf(5, 0, 0, 1, 127, 0, 0, 1, 0, 0))
        output.flush()

        val reader = input.bufferedReader(ISO_8859_1)
        val requestLine = reader.readLine()
        generateSequence(reader::readLine).first { it.isEmpty() }

        output.write("HTTP/1.1 200 OK\r\nContent-Length: ${CONTENT.size}\r\n\r\n".toByteArray(ISO_8859_1))
        output.write(CONTENT)
        output.flush()
        return requestLine
    }

    override fun close() {
        server.close()
        executor.shutdownNow()
    }

}

private fun InputStream.readRequired(): Int =
    read().takeIf { it >= 0 } ?: error("Unexpected end of stream")

private fun InputStream.readRequired(length: Int) {
    check(readNBytes(length).size == length) { "Unexpected end of stream" }
}
