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

import com.reposilite.repository.RepositoryFacade
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.infrastructure.ReposiliteEndpointFactory
import io.javalin.config.RouterConfig
import io.javalin.http.Handler
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.router.Endpoint
import io.javalin.router.ParsedEndpoint
import io.javalin.router.matcher.PathMatcher

internal class RepositoryEndpointRouter(
    private val repositoryFacade: RepositoryFacade,
    providerRoutes: Map<String, ReposiliteRoutes>,
    endpointFactory: ReposiliteEndpointFactory,
    routerConfig: RouterConfig,
) {

    private val providerEndpoints = providerRoutes.mapValues { (_, routes) ->
        endpointFactory.createEndpoints(routes)
    }

    private val providerRouters = providerEndpoints.mapValues { (_, endpoints) ->
        PathMatcher().apply {
            endpoints.forEach { endpoint -> add(ParsedEndpoint(endpoint, routerConfig)) }
        }
    }

    private val fallbackProvider = providerRoutes.keys.find { it == "maven" }

    private val gatewayHandler = createGatewayHandler()

    val gatewayEndpoints: Collection<Endpoint> = providerEndpoints.values
        .flatten()
        .map { it.method }
        .distinct()
        .flatMap { method ->
            listOf(
                Endpoint(method, "/{repository}", gatewayHandler),
                Endpoint(method, "/{repository}/<path>", gatewayHandler),
            )
        }

    private fun createGatewayHandler(): Handler =
        Handler { context ->
            val repositories = repositoryFacade.findRepositories(context.pathParam("repository"))
            val provider = when (repositories.size) {
                0 -> fallbackProvider
                1 -> repositories.single().provider.id
                else -> null
            }

            if (provider == null) {
                context.status(NOT_FOUND)
                return@Handler
            }

            val endpoint = providerRouters[provider]?.findFirstEntry(context.method(), context.path())
            if (endpoint == null) {
                context.status(NOT_FOUND)
                return@Handler
            }

            val servletContext = context as JavalinServletContext
            servletContext.update(endpoint.endpoint, endpoint.extractPathParams(context.path()))
            endpoint.endpoint.handler.handle(servletContext)
        }
}
