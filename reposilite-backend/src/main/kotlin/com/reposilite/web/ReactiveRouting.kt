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

package com.reposilite.web

import com.reposilite.Reposilite
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.response
import com.reposilite.web.http.uri
import com.reposilite.web.routing.AbstractRoutes
import com.reposilite.web.routing.ReactiveRoutingPlugin
import com.reposilite.web.routing.Route
import com.reposilite.web.routing.RouteMethod
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import kotlinx.coroutines.CoroutineDispatcher

abstract class ReposiliteRoutes : AbstractRoutes<ContextDsl, Unit>()

class ReposiliteRoute(
    path: String,
    vararg methods: RouteMethod,
    handler: suspend ContextDsl.() -> Unit
) : Route<ContextDsl, Unit>(path = path, methods = methods, handler = handler)

fun createReactiveRouting(reposilite: Reposilite, dispatcher: CoroutineDispatcher): ReactiveRoutingPlugin<ContextDsl, Unit> {
    val failureFacade = reposilite.failureFacade
    val authenticationFacade = reposilite.authenticationFacade

    val plugin = ReactiveRoutingPlugin<ContextDsl, Unit>(
        errorConsumer = { name, error -> reposilite.logger.error("Coroutine $name failed to execute task", error) },
        dispatcher = dispatcher,
        syncHandler = { ctx, route ->
            try {
                val dsl = ContextDsl(reposilite.logger, ctx, errorResponse(INTERNAL_SERVER_ERROR, "Unsupported sync auth"))
                route.handler(dsl)
                dsl.response?.also { ctx.response(it) }
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                failureFacade.throwException(ctx.uri(), throwable)
            }
        },
        asyncHandler = { ctx, route, result ->
            try {
                val authenticationResult = authenticationFacade.authenticateByHeader(ctx.headerMap())
                val dsl = ContextDsl(reposilite.logger, ctx, authenticationResult)
                route.handler(dsl)
                dsl.response?.also { ctx.response(it) }
                result.complete(Unit)
            }
            catch (throwable: Throwable) {
                result.completeExceptionally(throwable)
                throwable.printStackTrace()
                failureFacade.throwException(ctx.uri(), throwable)
            }
        }
    )

    reposilite.webs.forEach { web ->
        web.routing(reposilite).forEach {
            plugin.registerRoutes(it)
        }
    }

    return plugin
}