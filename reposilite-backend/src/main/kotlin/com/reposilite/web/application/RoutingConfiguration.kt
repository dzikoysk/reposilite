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

package com.reposilite.web.application

import com.reposilite.Reposilite
import com.reposilite.web.ContextDsl
import com.reposilite.web.http.response
import com.reposilite.web.http.uri
import com.reposilite.web.routing.AbstractRoutes
import com.reposilite.web.routing.Route
import com.reposilite.web.routing.RouteMethod
import com.reposilite.web.routing.RoutingPlugin

abstract class ReposiliteRoutes : AbstractRoutes<ContextDsl, Unit>()

class ReposiliteRoute(
    path: String,
    vararg methods: RouteMethod,
    handler: ContextDsl.() -> Unit
) : Route<ContextDsl, Unit>(path = path, methods = methods, handler = handler)

fun createReactiveRouting(reposilite: Reposilite): RoutingPlugin<ContextDsl, Unit> {
    val failureFacade = reposilite.failureFacade

    val plugin = RoutingPlugin<ContextDsl, Unit>(
        handler = { ctx, route ->
            try {
                val dsl = ContextDsl(reposilite.logger, ctx, lazy { reposilite.authenticationFacade.authenticateByHeader(ctx.headerMap()) })
                route.handler(dsl)
                dsl.response?.also { ctx.response(it) }
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                failureFacade.throwException(ctx.uri(), throwable)
            }
        }
    )

    reposilite.webs.asSequence()
        .flatMap { it.routing(reposilite) }
        .flatMap { it.routes }
        .distinctBy { it.methods.joinToString(";") + ":" + it.path }
        .toSet()
        .let { plugin.registerRoutes(it) }

    return plugin
}