package com.reposilite.web

import com.reposilite.Reposilite
import com.reposilite.ReposiliteWebConfiguration
import com.reposilite.auth.AuthenticationFacade
import com.reposilite.journalist.Journalist
import com.reposilite.web.http.response
import com.reposilite.web.routing.AbstractRoutes
import com.reposilite.web.routing.ReactiveRoutingPlugin
import com.reposilite.web.routing.Route
import com.reposilite.web.routing.RouteMethod
import io.javalin.http.Context

abstract class ReposiliteRoutes : AbstractRoutes<ContextDsl, Unit>()

class ReposiliteRoute(
    path: String,
    vararg methods: RouteMethod,
    handler: suspend ContextDsl.() -> Unit
) : Route<ContextDsl, Unit>(path = path, methods = methods, handler = handler)

fun createReactiveRouting(reposilite: Reposilite): ReactiveRoutingPlugin<ContextDsl, Unit> {
    val authenticationFacade = reposilite.authenticationFacade

    val plugin = ReactiveRoutingPlugin<ContextDsl, Unit>(
        errorConsumer = { name, error -> reposilite.logger.error("Coroutine $name failed to execute task", error) },
        dispatcher = reposilite.ioDispatcher,
        syncHandler = { ctx, route ->
            val resultDsl = callWithContext(reposilite, authenticationFacade, ctx, route.handler)
            resultDsl.response?.also { ctx.response(it) }
        },
        asyncHandler = { ctx, route, result ->
            try {
                val dsl = callWithContext(reposilite, authenticationFacade, ctx, route.handler)
                dsl.response?.also { ctx.response(it) }
                result.complete(Unit)
            }
            catch (throwable: Throwable) {
                throwable.printStackTrace()
                result.completeExceptionally(throwable)
            }
        }
    )

    ReposiliteWebConfiguration.routing(reposilite).forEach { plugin.registerRoutes(it) }
    return plugin
}

private suspend fun callWithContext(journalist: Journalist, authenticationFacade: AuthenticationFacade, ctx: Context, handler: suspend ContextDsl.() -> Unit): ContextDsl {
    val authenticationResult = authenticationFacade.authenticateByHeader(ctx.headerMap())
    val dsl = ContextDsl(journalist.logger, ctx, authenticationResult)
    handler(dsl)
    return dsl
}
