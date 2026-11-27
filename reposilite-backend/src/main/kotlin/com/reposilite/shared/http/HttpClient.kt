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

import java.io.EOFException
import java.io.FilterInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.util.Locale.ENGLISH
import java.util.zip.GZIPInputStream

private const val MAX_REDIRECTS = 5

internal class HttpRequest(
    val method: String,
    val url: URL,
    val headers: Map<String, String> = emptyMap(),
    val connectTimeoutInMillis: Int,
    val readTimeoutInMillis: Int,
)

internal class HttpClient(
    private val userAgent: String,
    private val proxy: Proxy? = null,
) {

    fun execute(request: HttpRequest): HttpResponse =
        execute(request, 0)

    private fun execute(request: HttpRequest, redirects: Int): HttpResponse {
        val response = createResponse(request)
        val location = response.getHeader("Location")

        if (response.isRedirect() && location != null && redirects < MAX_REDIRECTS) {
            response.close()
            return execute(
                request = HttpRequest(
                    method = if (response.statusCode == HttpURLConnection.HTTP_SEE_OTHER) "GET" else request.method,
                    url = request.url.toURI().resolve(location).toURL(),
                    headers = request.headers.filterKeys { !it.equals("Authorization", ignoreCase = true) },
                    connectTimeoutInMillis = request.connectTimeoutInMillis,
                    readTimeoutInMillis = request.readTimeoutInMillis,
                ),
                redirects = redirects + 1,
            )
        }

        return response
    }

    private fun createResponse(request: HttpRequest): HttpResponse {
        val connection = when (proxy) {
            null -> request.url.openConnection()
            else -> request.url.openConnection(proxy)
        } as HttpURLConnection

        return try {
            connection.instanceFollowRedirects = false
            connection.requestMethod = request.method
            connection.connectTimeout = request.connectTimeoutInMillis
            connection.readTimeout = request.readTimeoutInMillis
            connection.setRequestProperty("User-Agent", userAgent)
            request.headers.forEach(connection::setRequestProperty)
            HttpResponse(connection)
        } catch (exception: Exception) {
            connection.disconnect()
            throw exception
        }
    }
}

internal class HttpResponse(private val connection: HttpURLConnection) : AutoCloseable {

    val url: URL = connection.url
    val statusCode: Int = connection.responseCode
    val contentType: String? = connection.contentType
    val contentEncoding: String? = connection.contentEncoding
    val contentLength: Long = connection.contentLengthLong

    fun getHeader(name: String): String? =
        connection.getHeaderField(name)

    fun openBody(): InputStream {
        return try {
            val raw = connection.inputStream
            val fixedLength = if (contentLength >= 0 && getHeader("Transfer-Encoding") == null) {
                FixedLengthBody(raw, contentLength)
            } else null
            val body = fixedLength ?: raw
            val content = when (contentEncoding?.trim()?.lowercase(ENGLISH)) {
                "gzip", "x-gzip" -> GZIPInputStream(body)
                else -> body
            }

            DisconnectingInputStream(content, this, fixedLength)
        } catch (exception: Exception) {
            close()
            throw exception
        }
    }

    override fun close() {
        connection.disconnect()
    }

    fun isRedirect(): Boolean =
        statusCode == HttpURLConnection.HTTP_MOVED_PERM ||
            statusCode == HttpURLConnection.HTTP_MOVED_TEMP ||
            statusCode == HttpURLConnection.HTTP_SEE_OTHER ||
            statusCode == 307 ||
            statusCode == 308

}

/** Counts the encoded response bytes, including bytes consumed by the gzip decoder. */
private class FixedLengthBody(private val source: InputStream, private val expected: Long) : InputStream() {

    private var received = 0L

    override fun read(): Int =
        source.read().also { if (it >= 0) received++ }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        source.read(buffer, offset, length).also { if (it > 0) received += it }

    override fun available(): Int =
        source.available()

    override fun close() =
        source.close()

    fun verifyComplete() {
        if (received != expected) {
            throw EOFException("Expected $expected bytes from upstream, received $received")
        }
    }

}

private class DisconnectingInputStream(
    delegate: InputStream,
    private val response: HttpResponse,
    private val fixedLength: FixedLengthBody?,
) : FilterInputStream(delegate) {

    override fun read(): Int =
        super.read().also { if (it == -1) fixedLength?.verifyComplete() }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { if (it == -1) fixedLength?.verifyComplete() }

    override fun close() {
        response.use { _ ->
            super.close()
        }
    }

}
