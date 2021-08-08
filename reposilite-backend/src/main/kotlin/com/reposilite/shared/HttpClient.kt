package com.reposilite.shared

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.reposilite.failure.api.ErrorResponse
import com.reposilite.failure.api.errorResponse
import com.reposilite.web.api.MimeTypes
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpCode.NOT_ACCEPTABLE
import io.javalin.http.HttpCode.REQUEST_TIMEOUT
import panda.std.Result
import java.io.InputStream
import java.net.SocketTimeoutException

interface RemoteClient {

    fun get(uri: String, connectTimeout: Int, readTimeout: Int): Result<InputStream, ErrorResponse>

}

class HttpRemoteClient : RemoteClient {

    private val httpRequestFactory = NetHttpTransport().createRequestFactory()

    override fun get(uri: String, connectTimeout: Int, readTimeout: Int): Result<InputStream, ErrorResponse> {
        try {
            val remoteRequest = httpRequestFactory.buildGetRequest(GenericUrl(uri))
            remoteRequest.throwExceptionOnExecuteError = false
            remoteRequest.connectTimeout = connectTimeout * 1000
            remoteRequest.readTimeout = readTimeout * 1000

            val remoteResponse = remoteRequest.execute()

            return when {
                remoteResponse.headers.contentType == MimeTypes.HTML -> errorResponse(NOT_ACCEPTABLE, "...")
                remoteResponse.isSuccessStatusCode.not() -> errorResponse(NOT_ACCEPTABLE, "...")
                else -> errorResponse(NOT_ACCEPTABLE, "...")
            }
        } catch (exception: Exception) {
            return if (exception is SocketTimeoutException) {
                errorResponse(REQUEST_TIMEOUT, "Host $uri is unavailable due to: ${exception.message}")
            } else {
                errorResponse(INTERNAL_SERVER_ERROR, "Proxy exception: ${exception.message}")
            }
        }
    }

}

abstract class StubRemoteClient : RemoteClient {

}