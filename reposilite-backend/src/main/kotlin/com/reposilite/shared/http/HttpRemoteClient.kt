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

import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.shared.http.AuthenticationMethod.BASIC
import com.reposilite.shared.http.AuthenticationMethod.CUSTOM_HEADER
import com.reposilite.shared.http.AuthenticationMethod.LOOPBACK_LINK
import com.reposilite.shared.toErrorResult
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.UNKNOWN_LENGTH
import com.reposilite.storage.getExtension
import io.javalin.http.ContentType
import io.javalin.http.HttpStatus.NOT_ACCEPTABLE
import panda.std.Result
import panda.std.asSuccess
import java.io.InputStream
import java.net.Proxy
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Base64

private const val HTTP_GET = "GET"
private const val HTTP_HEAD = "HEAD"
private const val USER_AGENT = "Reposilite"

interface RemoteClientProvider {

    val defaultClient: RemoteClient

    fun createClient(proxy: Proxy): RemoteClient

}

class HttpRemoteClientProvider(private val journalist: Journalist) : RemoteClientProvider {

    override val defaultClient: RemoteClient by lazy {
        HttpRemoteClient(journalist, null)
    }

    override fun createClient(proxy: Proxy): RemoteClient =
        HttpRemoteClient(journalist, proxy)

}

class HttpRemoteClient(private val journalist: Journalist, proxy: Proxy?) : RemoteClient, Journalist {

    private val httpClient = HttpClient(USER_AGENT, proxy)

    override fun head(uri: String, credentials: RemoteCredentials?, connectTimeoutInSeconds: Int, readTimeoutInSeconds: Int): Result<FileDetails, ErrorResponse> =
        execute(HTTP_HEAD, uri, credentials, connectTimeoutInSeconds, readTimeoutInSeconds) { response ->
            // Nexus can send misleading for client content-length of chunked responses
            // ~ https://github.com/dzikoysk/reposilite/issues/549
            val contentLength = response.contentLength
                .takeUnless { "gzip" == response.contentEncoding } // remove content-length header
                ?: UNKNOWN_LENGTH

            val contentType = response.contentType
                ?.let { ContentType.contentType(it) }
                ?: ContentType.contentTypeByExtension(uri.getExtension())
                ?: ContentType.APPLICATION_OCTET_STREAM

            val lastModified = response
                .getHeader("Last-Modified")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    try {
                        ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                    } catch (_: DateTimeParseException) {
                        null
                    }
                }

            response.close()
            DocumentInfo(
                name = uri.substringAfterLast('/'),
                contentType = contentType,
                contentLength = contentLength,
                lastModifiedTime = lastModified,
            ).asSuccess()
        }

    override fun get(uri: String, credentials: RemoteCredentials?, connectTimeoutInSeconds: Int, readTimeoutInSeconds: Int): Result<InputStream, ErrorResponse> {
        return execute(HTTP_GET, uri, credentials, connectTimeoutInSeconds, readTimeoutInSeconds) { response ->
            response.openBody().asSuccess()
        }
    }

    private fun <R> execute(
        method: String,
        uri: String,
        credentials: RemoteCredentials?,
        connectTimeout: Int,
        readTimeout: Int,
        consumer: (HttpResponse) -> Result<R, ErrorResponse>,
    ): Result<R, ErrorResponse> {
        val url = try {
            URI(uri).toURL()
        } catch (_: Exception) {
            return badRequestError("Invalid remote URI: $uri")
        }
        val response = try {
            httpClient.execute(
                HttpRequest(
                    method = method,
                    url = url,
                    headers = credentials.toHeaders(),
                    connectTimeoutInMillis = connectTimeout * 1000,
                    readTimeoutInMillis = readTimeout * 1000,
                )
            )
        } catch (exception: Exception) {
            return createExceptionResponse(url.toString(), exception)
        }

        return try {
            logger.debug("HttpRemoteClient | ${response.url} responded with ${response.statusCode} (Content-Type: ${response.contentType})")

            when {
                response.statusCode !in 200..299 -> when (response.statusCode) {
                    404, 429 -> Result.error(ErrorResponse(response.statusCode, "Unsuccessful request (${response.statusCode})"))
                    else -> NOT_ACCEPTABLE.toErrorResult("Unsuccessful request (${response.statusCode})")
                }
                response.contentType == ContentType.HTML -> NOT_ACCEPTABLE.toErrorResult("Illegal file type (${response.contentType})")
                else -> consumer(response)
            }.onError {
                response.close()
            }
        } catch (exception: Exception) {
            response.close()
            createExceptionResponse(url.toString(), exception)
        }
    }

    private fun RemoteCredentials?.toHeaders(): Map<String, String> =
        when (this?.method) {
            BASIC, LOOPBACK_LINK -> {
                val value = Base64.getEncoder().encodeToString("$login:$password".toByteArray(UTF_8))
                mapOf("Authorization" to "Basic $value")
            }
            CUSTOM_HEADER -> mapOf(login to password)
            null -> emptyMap()
        }

    private fun <V> createExceptionResponse(uri: String, exception: Exception): Result<V, ErrorResponse> {
        logger.debug("HttpRemoteClient | Cannot get $uri")
        logger.exception(Channel.DEBUG, exception)
        return badRequestError("An error of type ${exception.javaClass} happened: ${exception.message}")
    }

    override fun getLogger(): Logger =
        journalist.logger

}
