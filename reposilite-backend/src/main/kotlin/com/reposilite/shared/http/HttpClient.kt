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
            val content = when (contentEncoding?.trim()?.lowercase(ENGLISH)) {
                "gzip", "x-gzip" -> GZIPInputStream(connection.inputStream)
                else -> connection.inputStream
            }

            DisconnectingInputStream(content, this)
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

private class DisconnectingInputStream(delegate: InputStream, private val response: HttpResponse) : FilterInputStream(delegate) {

    override fun close() {
        response.use { _ ->
            super.close()
        }
    }

}
