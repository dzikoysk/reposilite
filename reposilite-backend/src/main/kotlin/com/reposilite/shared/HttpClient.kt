package com.reposilite.shared

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.shared.FilesUtils.getExtension
import com.reposilite.web.http.ContentType
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.BAD_REQUEST
import io.javalin.http.HttpCode.NOT_ACCEPTABLE
import org.eclipse.jetty.http.HttpHeader
import panda.std.Result
import panda.std.asSuccess

interface RemoteClient {

    suspend fun get(uri: String, connectTimeout: Int, readTimeout: Int): Result<DocumentInfo, ErrorResponse>

}

class HttpRemoteClient : RemoteClient {

    override suspend fun get(uri: String, connectTimeout: Int, readTimeout: Int): Result<DocumentInfo, ErrorResponse> =
        Fuel.get("uri")
            .timeout(connectTimeout * 1000)
            .timeoutRead(readTimeout * 1000)
            .awaitByteArrayResponseResult()
            .let { (_, response, result) ->
                result.fold(
                    { data -> get(uri, response, data) },
                    { error -> errorResponse(BAD_REQUEST, "An error of type ${error.exception} happened: ${error.message}")}
                )
            }

    private fun get(uri: String, response: Response, data: ByteArray): Result<DocumentInfo, ErrorResponse> {
        val contentType = response.header(HttpHeader.CONTENT_TYPE.asString())
            .firstOrNull()
            ?.let { ContentType.getContentType(it) }
            ?: ContentType.getContentTypeByExtension(uri.getExtension())
            ?: ContentType.APPLICATION_OCTET_STREAM

        val contentLength = response.contentLength

        return when {
            contentType == ContentType.TEXT_HTML -> errorResponse(NOT_ACCEPTABLE, "Illegal file type")
            response.isSuccessful.not() -> errorResponse(NOT_ACCEPTABLE, "Unsuccessful request")
            else -> {
                DocumentInfo(
                    uri.toNormalizedPath().get().getSimpleName(),
                    contentType,
                    contentLength,
                    { data.inputStream() }
                ).asSuccess()
            }
        }
    }

}

class FakeRemoteClient(
    private val handler: suspend (String, Int, Int) -> Result<DocumentInfo, ErrorResponse>
) : RemoteClient {

    override suspend fun get(uri: String, connectTimeout: Int, readTimeout: Int): Result<DocumentInfo, ErrorResponse> =
        handler(uri, connectTimeout, readTimeout)

}