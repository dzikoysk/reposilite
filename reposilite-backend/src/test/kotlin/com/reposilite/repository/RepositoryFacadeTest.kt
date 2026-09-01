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
import com.reposilite.repository.api.Repository
import com.reposilite.repository.api.RepositoryProvider
import com.reposilite.repository.api.RepositoryVisibility.PUBLIC
import com.reposilite.storage.api.Location
import com.reposilite.token.application.AccessTokenComponents
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.BEFORE
import io.javalin.community.routing.Route.GET
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import panda.std.reactive.Reference
import panda.std.reactive.mutableReference
import panda.std.reactive.toReference

internal class RepositoryFacadeTest {

    private val emptyRoutes = object : ReposiliteRoutes() {
        override val routes = emptySet<ReposiliteRoute<*>>()
    }

    private val facade = RepositoryFacade(
        accessResolver = RepositoryAccessResolver(
            AccessTokenComponents(InMemoryLogger(), null).accessTokenFacade()
        ),
    )

    @Test
    fun `should resolve repository from registered provider`() {
        // given: two providers with distinct repositories
        facade.registerProvider(provider("maven", "releases"))
        facade.registerProvider(provider("custom", "downloads"))

        // when: repository is resolved by name
        val provided = facade.findRepository("downloads")

        // then: owning provider and repository are returned
        assertThat(provided?.provider?.id).isEqualTo("custom")
        assertThat(provided?.repository?.name).isEqualTo("downloads")
    }

    @Test
    fun `should reflect repositories changed by provider`() {
        // given: provider backed by a repository reference
        val repositories = mutableReference<Collection<Repository>>(listOf(repository("first")))
        facade.registerProvider(provider("custom", repositories))

        // when: provider configuration replaces its repositories
        repositories.update(listOf(repository("second")))

        // then: facade queries the provider instead of serving a stale index
        assertThat(facade.findRepository("first")).isNull()
        assertThat(facade.findRepository("second")?.repository?.name).isEqualTo("second")
    }

    @Test
    fun `should reject duplicate provider id`() {
        facade.registerProvider(provider("custom", "first"))

        assertThatIllegalArgumentException()
            .isThrownBy { facade.registerProvider(provider("custom", "second")) }
            .withMessageContaining("already registered")
    }

    @Test
    fun `should hide repository name shared by providers`() {
        val customRepositories = mutableReference<Collection<Repository>>(listOf(repository("shared")))
        facade.registerProvider(provider("maven", "shared"))
        facade.registerProvider(provider("custom", customRepositories))

        assertThat(facade.findRepository("shared")).isNull()
        assertThat(facade.getRepositories()).isEmpty()

        customRepositories.update(emptyList())

        assertThat(facade.findRepository("shared")?.provider?.id).isEqualTo("maven")
        assertThat(facade.getRepositories()).hasSize(1)
    }

    @Test
    fun `should reject providers registered after routing is sealed`() {
        facade.registerProvider(provider("maven", "releases"))
        facade.validateAndSeal()

        assertThatIllegalStateException()
            .isThrownBy { facade.registerProvider(provider("cargo", "cargo-releases")) }
            .withMessageContaining("before the HTTP server starts")
    }

    @Test
    fun `should reject repository names that cannot be routed safely`() {
        assertThatIllegalArgumentException()
            .isThrownBy { facade.validateRepositoryName("../downloads") }
            .withMessageContaining("URL path segment")
    }

    @Test
    fun `should reject provider routes outside repository gateway`() {
        val invalidRoutes = object : ReposiliteRoutes() {
            override val routes = routes(ReposiliteRoute<Unit>("/cargo/<crate>", GET) {})
        }
        facade.registerProvider(provider("cargo", listOf(repository("cargo")).toReference(), invalidRoutes))

        assertThatIllegalArgumentException()
            .isThrownBy { facade.validateAndSeal() }
            .withMessageContaining("/{repository}")
    }

    @Test
    fun `should reject provider filter routes`() {
        val invalidRoutes = object : ReposiliteRoutes() {
            override val routes = routes(ReposiliteRoute<Unit>("/{repository}/<path>", BEFORE) {})
        }
        facade.registerProvider(provider("cargo", listOf(repository("cargo")).toReference(), invalidRoutes))

        assertThatIllegalArgumentException()
            .isThrownBy { facade.validateAndSeal() }
            .withMessageContaining("HTTP methods only")
    }

    @Test
    fun `should reject current directory path before checking access`() {
        val repository = repository("cargo")
        val currentDirectory = Location.of(".")

        assertThat(facade.canAccessResource(null, repository, currentDirectory).error.status).isEqualTo(400)
        assertThat(facade.canModifyResource(null, repository, currentDirectory)).isFalse
    }

    private fun provider(id: String, vararg names: String): RepositoryProvider =
        provider(id, names.map { repository(it) }.toReference())

    private fun provider(id: String, repositories: Reference<out Collection<Repository>>): RepositoryProvider =
        provider(id, repositories, emptyRoutes)

    private fun provider(id: String, repositories: Reference<out Collection<Repository>>, endpoints: ReposiliteRoutes): RepositoryProvider =
        object : RepositoryProvider {
            override val id = id
            private val repositoryReference = repositories.computed<Collection<Repository>> { it }

            override fun routes(): ReposiliteRoutes =
                endpoints

            override fun repositories(): Reference<Collection<Repository>> =
                repositoryReference
        }

    private fun repository(id: String): Repository =
        object : Repository {
            override val name = id
            override val visibility = PUBLIC
        }
}
