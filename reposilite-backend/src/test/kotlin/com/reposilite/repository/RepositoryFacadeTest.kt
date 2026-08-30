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
import com.reposilite.token.application.AccessTokenComponents
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.BEFORE
import io.javalin.community.routing.Route.GET
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

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
        // given: provider backed by a mutable repository map
        val repositories = linkedMapOf("first" to repository("first"))
        facade.registerProvider(provider("custom", repositories))

        // when: provider configuration replaces its repositories
        repositories.clear()
        repositories["second"] = repository("second")

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
    fun `should reject repository name shared by providers`() {
        facade.registerProvider(provider("maven", "shared"))
        facade.registerProvider(provider("custom", "shared"))

        assertThatIllegalArgumentException()
            .isThrownBy { facade.findRepository("shared") }
            .withMessageContaining("provided by multiple providers")

        assertThatIllegalArgumentException()
            .isThrownBy { facade.getRepositories() }
            .withMessageContaining("unique across providers")
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
    fun `should accept protocol specific resource paths`() {
        val repository = repository("oci-releases")

        val result = facade.canAccessResource(
            accessToken = null,
            repository = repository,
            resourcePath = "v2/content/library/blobs/sha256:123456",
        )

        assertThat(result.isOk).isTrue
    }

    @Test
    fun `should reject repository names that cannot be routed safely`() {
        assertThatIllegalArgumentException()
            .isThrownBy { facade.validateRepositoryName("custom", "../downloads") }
            .withMessageContaining("URL path segment")
    }

    @Test
    fun `should reject repository name owned by another provider`() {
        facade.registerProvider(provider("maven", "releases"))

        assertThatIllegalArgumentException()
            .isThrownBy { facade.validateRepositoryName("custom", "releases") }
            .withMessageContaining("already provided by 'maven'")
    }

    @Test
    fun `should reject provider routes outside repository gateway`() {
        val invalidRoutes = object : ReposiliteRoutes() {
            override val routes = routes(ReposiliteRoute<Unit>("/cargo/<crate>", GET) {})
        }
        facade.registerProvider(provider("cargo", mapOf("cargo" to repository("cargo")), invalidRoutes))

        assertThatIllegalArgumentException()
            .isThrownBy { facade.validateAndSeal() }
            .withMessageContaining("/{repository}")
    }

    @Test
    fun `should reject provider filter routes`() {
        val invalidRoutes = object : ReposiliteRoutes() {
            override val routes = routes(ReposiliteRoute<Unit>("/{repository}/<path>", BEFORE) {})
        }
        facade.registerProvider(provider("cargo", mapOf("cargo" to repository("cargo")), invalidRoutes))

        assertThatIllegalArgumentException()
            .isThrownBy { facade.validateAndSeal() }
            .withMessageContaining("HTTP methods only")
    }

    @Test
    fun `should reject non-canonical paths before checking access`() {
        val repository = repository("cargo")

        assertThat(facade.canAccessResource(null, repository, "public/../private").error.status).isEqualTo(400)
        assertThat(facade.canModifyResource(null, repository, "public\\..\\private")).isFalse
    }

    private fun provider(id: String, vararg names: String): RepositoryProvider =
        provider(id, names.associateWithTo(linkedMapOf()) { repository(it) })

    private fun provider(id: String, repositories: Map<String, Repository>): RepositoryProvider =
        provider(id, repositories, emptyRoutes)

    private fun provider(id: String, repositories: Map<String, Repository>, routes: ReposiliteRoutes): RepositoryProvider =
        object : RepositoryProvider {
            override val id = id
            override val routes = routes

            override fun findRepository(name: String): Repository? =
                repositories[name]

            override fun getRepositories(): Collection<Repository> =
                repositories.values
        }

    private fun repository(id: String): Repository =
        object : Repository {
            override val name = id
            override val visibility = PUBLIC
        }
}
