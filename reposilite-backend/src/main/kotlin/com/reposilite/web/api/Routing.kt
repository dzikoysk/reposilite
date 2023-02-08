/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.web.api

import com.reposilite.shared.ContextDsl
import com.reposilite.web.routing.RouteMethod
import io.javalin.community.routing.Route
import io.javalin.community.routing.dsl.DefaultDslRoute
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.dsl.DslRoutes

abstract class ReposiliteRoutes : DslRoutes<DslRoute<ContextDsl<*>, Unit>, ContextDsl<*>, Unit> {

    abstract val routes: Set<ReposiliteRoute<*>>

    @Suppress("UNCHECKED_CAST")
    fun routes(vararg reposiliteRoutes: ReposiliteRoute<*>): Set<ReposiliteRoute<Any>> =
        reposiliteRoutes
            .map { it as ReposiliteRoute<Any> }
            .toSet()

    @Suppress("UNCHECKED_CAST")
    override fun routes(): Collection<DslRoute<ContextDsl<*>, Unit>> =
        routes.flatMap { route ->
            route.methods.map { method ->
                DefaultDslRoute(
                    path = route.path,
                    method = method,
                    handler = route.handler as ContextDsl<*>.() -> Unit
                )
            }
        }

}

class ReposiliteRoute<R>(
    val path: String,
    vararg val methods: Route,
    val handler: ContextDsl<R>.() -> Unit
) {

    @Deprecated("Use io.javalin.community.routing.Route instead of RouteMethod")
    constructor(
        path: String,
        vararg methods: RouteMethod,
        handler: ContextDsl<R>.() -> Unit
    ) : this(
        path = path,
        methods = methods
            .map { Route.valueOf(it.name) }
            .toTypedArray()
        ,
        handler = handler
    )

}
