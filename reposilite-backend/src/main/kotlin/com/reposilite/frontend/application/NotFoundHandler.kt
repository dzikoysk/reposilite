package com.reposilite.frontend.application

import com.reposilite.frontend.FrontendFacade
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.http.NotFoundResponse

internal class NotFoundHandler(private val frontendFacade: FrontendFacade) : Handler, ExceptionHandler<NotFoundResponse> {

    private val handler: (Context) -> Unit = { ctx ->
        if (ctx.resultString() == null && ctx.resultStream() == null) {
            ctx.html(frontendFacade.createNotFoundPage(ctx.req.requestURI))
        }
    }

    override fun handle(ctx: Context) =
        handleNotFound(ctx)

    override fun handle(exception: NotFoundResponse, ctx: Context) =
        handleNotFound(ctx)

    private fun handleNotFound(ctx: Context) {
        if (ctx.resultFuture() != null) {
            ctx.resultFuture()!!.thenRun { handler(ctx) }
        }
        else {
            handler(ctx)
        }
    }

}