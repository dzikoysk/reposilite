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

package com.reposilite.repository

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.repository.api.RepositoryInfo
import com.reposilite.repository.api.RepositoryProvider
import com.reposilite.repository.api.RepositoryVisibility.PUBLIC
import com.reposilite.repository.infrastructure.RepositoryDispatcher
import com.reposilite.token.application.AccessTokenComponents
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.infrastructure.ReposiliteDsl
import io.javalin.Javalin
import io.javalin.community.routing.Route.GET
import io.javalin.http.Handler
import kong.unirest.core.Unirest.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RepositoryRoutingIntegrationTest {

    private val facade = RepositoryFacade(
        RepositoryAccessResolver(AccessTokenComponents(InMemoryLogger(), null).accessTokenFacade()),
    )

    private val routes = object : ReposiliteRoutes() {
        override val routes = routes(ReposiliteRoute<Unit>("/{repository}/<resource>", GET) {})
    }

    @Test
    fun `should route repositories changed after startup`() {
        // given: a running server with a registered provider
        val provider = register("custom", "first")
        useServer { base ->
            assertThat(get("$base/first/pkg/file").asString().body).isEqualTo("first/pkg/file")

            // when: the provider replaces its repositories
            provider.names = listOf("second")

            // then: routing uses the new repositories and extracts the protocol path parameters
            assertThat(get("$base/first/pkg/file").asEmpty().status).isEqualTo(404)
            assertThat(get("$base/second/pkg/file").asString().body).isEqualTo("second/pkg/file")
        }
    }

    @Test
    fun `should reject conflicting repositories until the conflict is removed`() {
        // given: two providers exposing the same repository name
        register("custom", "shared")
        val other = register("other", "shared")
        useServer { base ->
            // when: the conflicting repository is requested
            val response = get("$base/shared/file").asEmpty()

            // then: neither provider is selected
            assertThat(response.status).isEqualTo(404)

            // when: one provider removes its conflicting repository
            other.names = emptyList()

            // then: the remaining repository is accessible without restarting
            assertThat(get("$base/shared/file").asString().body).isEqualTo("shared/file")
        }
    }

    @Test
    fun `should reject duplicate names from the same provider`() {
        // given: one provider exposing a repository name twice
        register("custom", "shared", "shared")
        useServer { base ->
            // when: the ambiguous repository is requested
            val response = get("$base/shared/file").asEmpty()

            // then: routing does not select either repository
            assertThat(response.status).isEqualTo(404)
        }
    }

    @Test
    fun `should keep running routes and providers on the same registration snapshot`() {
        // given: a server started with one provider
        register("custom", "downloads")
        useServer { base ->
            // when: another type registers a conflicting repository after startup
            register("late", "downloads")

            // then: the new provider does not affect the running server's routes
            assertThat(get("$base/downloads/file").asString().body).isEqualTo("downloads/file")
        }
    }

    private fun register(type: String, vararg names: String): TestRepositoryProvider =
        TestRepositoryProvider(names.toList()).also {
            assertThat(facade.register(type, routes, it).isOk).isTrue
        }

    private fun useServer(block: (String) -> Unit) {
        val server = Javalin.create { config ->
            val dispatcher = RepositoryDispatcher(
                registrations = facade.getRegistrations(),
                dsl = ReposiliteDsl(
                    routeFactory = { Handler { context ->
                        context.result("${context.pathParam("repository")}/${context.pathParam("resource")}")
                    } },
                    exceptionRouteFactory = { error("No exception handlers are registered") },
                ),
                routerConfig = config.router,
            )
            dispatcher.endpoints.forEach { config.routes.addEndpoint(it) }
        }.start(0)

        try {
            block("http://localhost:${server.port()}")
        } finally {
            server.stop()
        }
    }

    private class TestRepositoryProvider(var names: List<String>) : RepositoryProvider {
        override fun getRepositories(): Collection<RepositoryInfo> =
            names.map { name ->
                object : RepositoryInfo {
                    override val name = name
                    override val visibility = PUBLIC
                }
            }
    }
}
