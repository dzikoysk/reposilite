package com.reposilite.shared

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.shared.FilesUtils.getExtension
import com.reposilite.web.api.MimeTypes
import com.reposilite.web.error.ErrorResponse
import com.reposilite.web.error.errorResponse
import com.reposilite.web.mimetypes.ContentType.BIN
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
                    { data ->
                        val contentType = response.header(HttpHeader.CONTENT_TYPE.asString()).firstOrNull() ?: MimeTypes.getMimeType(getExtension(uri))

                        when {
                            contentType == MimeTypes.HTML -> errorResponse(NOT_ACCEPTABLE, "Illegal file type")
                            response.isSuccessful.not() -> errorResponse(NOT_ACCEPTABLE, "Unsuccessful request")
                            else -> {
                                DocumentInfo(
                                    uri.toNormalizedPath().get().getSimpleName(),
                                    BIN,
                                    response.contentLength,
                                    { data.inputStream() }
                                ).asSuccess()
                            }
                        }
                    },
                    { error -> errorResponse(BAD_REQUEST, "An error of type ${error.exception} happened: ${error.message}")}
                )
            }

}

class FakeRemoteClient(
    private val handler: suspend (String, Int, Int) -> Result<DocumentInfo, ErrorResponse>
) : RemoteClient {

    override suspend fun get(uri: String, connectTimeout: Int, readTimeout: Int): Result<DocumentInfo, ErrorResponse> =
        handler(uri, connectTimeout, readTimeout)

}