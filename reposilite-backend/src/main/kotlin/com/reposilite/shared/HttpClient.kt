/*
 * Copyright (c) 2021 dzikoysk
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

package com.reposilite.shared

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpMethods
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.javanet.NetHttpTransport
import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.FileDetails
import com.reposilite.maven.api.UNKNOWN_LENGTH
import com.reposilite.shared.FilesUtils.getExtension
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.ContentType
import io.javalin.http.HttpCode.BAD_REQUEST
import io.javalin.http.HttpCode.NOT_ACCEPTABLE
import panda.std.Result
import panda.std.asSuccess
import java.io.InputStream


interface RemoteClient {

    /**
     * @param uri - full remote host address with a gav
     * @param credentials - basic credentials in user:password format
     * @param connectTimeout - connection establishment timeout in seconds
     * @param readTimeout - connection read timeout in seconds
     */
    fun head(uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): Result<FileDetails, ErrorResponse>

    /**
     * @param uri - full remote host address with a gav
     * @param credentials - basic credentials in user:password format
     * @param connectTimeout - connection establishment timeout in seconds
     * @param readTimeout - connection read timeout in seconds
     */
    fun get(uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): Result<InputStream, ErrorResponse>

}

class HttpRemoteClient(private val journalist: Journalist) : RemoteClient {

    private val requestFactory = NetHttpTransport().createRequestFactory()

    override fun head(uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): Result<FileDetails, ErrorResponse> =
        try {
            val request = createRequest(HttpMethods.HEAD, uri, credentials, connectTimeout, readTimeout)
            val response = request.execute()

            when {
                response.contentType == ContentType.HTML -> errorResponse(NOT_ACCEPTABLE, "Illegal file type")
                response.isSuccessStatusCode.not() -> errorResponse(NOT_ACCEPTABLE, "Unsuccessful request")
                else -> {
                    val headers = response.headers

                    // Nexus can send misleading for client content-length of chunked responses
                    // ~ https://github.com/dzikoysk/reposilite/issues/549
                    val contentLength =
                        if ("gzip" == headers.contentEncoding)
                            UNKNOWN_LENGTH // remove content-length header
                        else
                            headers.contentLength

                    val contentType = headers.contentType
                        ?.let { ContentType.getContentType(it) }
                        ?: ContentType.getContentTypeByExtension(uri.getExtension())
                        ?: ContentType.APPLICATION_OCTET_STREAM

                    DocumentInfo(
                        uri.toPath().getSimpleName(),
                        contentType,
                        contentLength
                    ).asSuccess()
                }
            }
        } catch (exception: Exception) {
            createErrorResponse(uri, exception)
        }

    override fun get(uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): Result<InputStream, ErrorResponse> =
        try {
            val request = createRequest(HttpMethods.GET, uri, credentials, connectTimeout, readTimeout)
            val response = request.execute()
            response.content.asSuccess()
        } catch (exception: Exception) {
            createErrorResponse(uri, exception)
        }

    private fun createRequest(method: String, uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): HttpRequest {
        val request = requestFactory.buildRequest(method, GenericUrl(uri), null)
        request.throwExceptionOnExecuteError = false
        request.connectTimeout = connectTimeout * 1000
        request.readTimeout = readTimeout * 1000
        request.authenticateWith(credentials)
        return request
    }

    private fun <V> createErrorResponse(uri: String, exception: Exception): Result<V, ErrorResponse> {
        journalist.logger.debug("Cannot get $uri")
        journalist.logger.exception(Channel.DEBUG, exception)
        return errorResponse(BAD_REQUEST, "An error of type ${exception.javaClass} happened: ${exception.message}")
    }

    private fun HttpRequest.authenticateWith(credentials: String?): HttpRequest = also {
        if (credentials != null) {
            val (username, password) = credentials.split(":", limit = 2)
            it.headers.setBasicAuthentication(username, password)
        }
    }

}

class FakeRemoteClient(
    private val headHandler: (String, String?, Int, Int) -> Result<FileDetails, ErrorResponse>,
    private val getHandler: (String, String?, Int, Int) -> Result<InputStream, ErrorResponse>
) : RemoteClient {

    override fun head(uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): Result<FileDetails, ErrorResponse> =
        headHandler(uri, credentials, connectTimeout, readTimeout)

    override fun get(uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): Result<InputStream, ErrorResponse> =
        getHandler(uri, credentials, connectTimeout, readTimeout)

}