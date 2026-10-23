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

package com.reposilite.repository.specification

import com.reposilite.repository.infrastructure.RepositoryRoutingPlugin
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.infrastructure.ReposiliteDsl
import io.javalin.Javalin
import io.javalin.community.routing.Route.GET
import io.javalin.http.Handler
import panda.std.ResultAssertions.assertOk

internal abstract class RepositoryRoutingSpecification : RepositorySpecification() {

    private val routes = useRoutes(ReposiliteRoute<Unit>("/{repository}/<resource>", GET) {})

    protected fun useRepositoryType(type: String, vararg names: String): TestRepositoryProvider =
        useRepositoryProvider(*names).also {
            assertOk(repositoryFacade.register(type, routes, it))
        }

    protected fun useServer(block: (String) -> Unit) {
        val server = Javalin.create { config ->
            config.router.handlerWrapper { endpoint ->
                Handler { context ->
                    context.header("X-Route", endpoint.path)
                    endpoint.handler.handle(context)
                }
            }
            config.registerPlugin(
                RepositoryRoutingPlugin.create(
                    registrations = repositoryFacade.getRegistrations(),
                    dsl = ReposiliteDsl(
                        routeFactory = { Handler { context ->
                            context.result("${context.pathParam("repository")}/${context.pathParam("resource")}")
                        } },
                        exceptionRouteFactory = { error("No exception handlers are registered") },
                    ),
                    routerConfig = config.router,
                )
            )
        }

        try {
            server.start(0)
            block("http://localhost:${server.port()}")
        } finally {
            server.stop()
        }
    }

}
