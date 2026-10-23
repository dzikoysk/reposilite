/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.repository.infrastructure

import com.reposilite.repository.RepositoryFacade.Registration
import com.reposilite.web.infrastructure.ReposiliteDsl
import io.javalin.config.JavalinState
import io.javalin.config.RouterConfig
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.plugin.Plugin
import io.javalin.router.Endpoint
import io.javalin.router.ParsedEndpoint
import io.javalin.router.matcher.PathMatcher

internal class RepositoryRoutingPlugin private constructor(
    private val registrations: Map<String, Registration>,
    private val routersByType: Map<String, PathMatcher>,
) : Plugin<Unit?>() {

    companion object {

        fun create(
            registrations: Map<String, Registration>,
            dsl: ReposiliteDsl,
            routerConfig: RouterConfig,
        ): RepositoryRoutingPlugin {
            val routersByType = registrations.mapValues { (_, registration) ->
                PathMatcher().apply {
                    dsl.createEndpoints(registration.routes).forEach { endpoint ->
                        add(ParsedEndpoint(endpoint, routerConfig))
                    }
                }
            }

            return RepositoryRoutingPlugin(
                registrations = registrations,
                routersByType = routersByType,
            )
        }

    }

    override fun onStart(state: JavalinState) {
        val handler = Handler(::dispatch)
        routersByType
            .values
            .flatMap { it.allEntries() }
            .flatMap {
                when (val method = it.endpoint.method) {
                    GET -> listOf(GET, HEAD)
                    else -> listOf(method)
                }
            }
            .distinct()
            .forEach { method ->
                state.routes.addEndpoint(Endpoint(method, "/{repository}", handler))
                state.routes.addEndpoint(Endpoint(method, "/{repository}/<path>", handler))
            }
    }

    private fun dispatch(context: Context) {
        val type = resolveRepositoryType(context.pathParam("repository"))
        val router = routersByType[type]
        val endpoint = router?.findFirstEntry(context.method(), context.path())

        when {
            endpoint != null -> endpoint.handle(context as JavalinServletContext, context.path())
            context.method() == HEAD && router?.hasEntries(GET, context.path()) == true -> context.status(OK)
            else -> context.status(NOT_FOUND)
        }
    }

    private fun resolveRepositoryType(name: String): String? {
        val types = registrations.flatMap { (type, registration) ->
            registration.provider.getRepositories()
                .filter { it.name == name }
                .map { type }
        }

        return when (types.size) {
            0 -> "maven"
            1 -> types.single()
            else -> null
        }
    }

}
