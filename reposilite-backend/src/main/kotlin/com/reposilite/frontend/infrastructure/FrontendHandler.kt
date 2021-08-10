package com.reposilite.frontend.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.web.ReposiliteRoutes
import com.reposilite.web.context.encoding
import io.javalin.http.Context
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.MimeTypes
import java.io.InputStream

internal abstract class FrontendHandler(protected val frontendFacade: FrontendFacade) : ReposiliteRoutes {

    protected fun respondWithFile(ctx: Context, uri: String, inputStream: InputStream?) {
        inputStream
            ?.let {
                ctx.result(it) // -> ctx.result(it.readBytes().decodeToString())
                    .encoding(Charsets.UTF_8)
                    .contentType(MimeTypes.getDefaultMimeByExtension(uri))
            }
            ?: ctx.status(HttpStatus.NOT_FOUND_404)
    }

}