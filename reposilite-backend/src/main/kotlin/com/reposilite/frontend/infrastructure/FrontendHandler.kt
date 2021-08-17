package com.reposilite.frontend.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.shared.FilesUtils.getExtension
import com.reposilite.web.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.encoding
import io.javalin.http.ContentType
import io.javalin.http.ContentType.Companion.PLAIN
import io.javalin.http.Context
import io.javalin.http.HttpCode.NOT_FOUND
import panda.std.Result
import java.io.InputStream
import kotlin.text.Charsets.UTF_8

internal abstract class FrontendHandler(protected val frontendFacade: FrontendFacade) : ReposiliteRoutes() {

    protected fun respondWithFile(ctx: Context, uri: String, inputStream: InputStream?): Result<String, ErrorResponse> =
        Result.`when`(inputStream != null,
            {
                ctx.encoding(UTF_8)
                ctx.contentType(ContentType.getMimeTypeByExtension(uri.getExtension()) ?: PLAIN)
                inputStream!!.readBytes().decodeToString() // TODO: Replace with raw InputStream as soon as Javalin won't throw exceptions in debug mode
            },
            { ErrorResponse(NOT_FOUND, "Resource not found") },
        )

}