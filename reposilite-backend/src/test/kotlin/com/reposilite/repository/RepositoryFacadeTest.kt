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
import com.reposilite.storage.api.Location
import com.reposilite.token.application.AccessTokenComponents
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.BEFORE
import io.javalin.community.routing.Route.GET
import org.assertj.core.api.Assertions.assertThat
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
    fun `should reject blank repository type`() {
        // given: a provider with a valid repository
        val provider = TestRepositoryProvider(listOf(repository("downloads")))

        // when: registration uses a blank type
        val result = facade.register(" ", emptyRoutes, provider)

        // then: the error is returned without adding a registration
        assertThat(result.error).contains("cannot be blank")
        assertThat(facade.getRegistrations()).isEmpty()
    }

    @Test
    fun `should reject duplicate repository type`() {
        // given: an already registered repository type
        val provider = TestRepositoryProvider(listOf(repository("first")))
        facade.register("custom", emptyRoutes, provider)

        // when: another provider registers the same type
        val result = facade.register("custom", emptyRoutes, TestRepositoryProvider(listOf(repository("second"))))

        // then: the error is returned and the original provider is retained
        assertThat(result.error).contains("already registered")
        assertThat(facade.getRegistrations().getValue("custom").provider).isSameAs(provider)
    }

    @Test
    fun `should reject routes outside repository gateway`() {
        // given: a repository route without the repository path parameter
        val invalidRoutes = object : ReposiliteRoutes() {
            override val routes = routes(ReposiliteRoute<Unit>("/cargo/<crate>", GET) {})
        }
        val provider = TestRepositoryProvider(listOf(repository("cargo")))

        // when: the invalid routes are registered
        val result = facade.register("cargo", invalidRoutes, provider)

        // then: the error is returned and the type is available for a corrected registration
        assertThat(result.error).contains("/{repository}")
        assertThat(facade.getRegistrations()).isEmpty()
        assertThat(facade.register("cargo", emptyRoutes, provider).isOk).isTrue
    }

    @Test
    fun `should reject filter routes`() {
        // given: a repository registration containing a filter route
        val invalidRoutes = object : ReposiliteRoutes() {
            override val routes = routes(ReposiliteRoute<Unit>("/{repository}/<path>", BEFORE) {})
        }

        // when: a filter is registered as a repository route
        val result = facade.register("cargo", invalidRoutes, TestRepositoryProvider(listOf(repository("cargo"))))

        // then: the error is returned without adding a registration
        assertThat(result.error).contains("HTTP methods only")
        assertThat(facade.getRegistrations()).isEmpty()
    }

    @Test
    fun `should reject current directory path before checking access`() {
        // given: a repository and the current directory path
        val repository = repository("cargo")
        val currentDirectory = Location.of(".")

        // when: resource access is resolved
        val access = facade.canAccessResource(null, repository, currentDirectory)
        val modification = facade.canModifyResource(null, repository, currentDirectory)

        // then: access is rejected before repository permissions are evaluated
        assertThat(access.error.status).isEqualTo(400)
        assertThat(modification).isFalse
    }

    private fun repository(id: String): RepositoryInfo =
        object : RepositoryInfo {
            override val name = id
            override val visibility = PUBLIC
        }

    private class TestRepositoryProvider(private val repositories: Collection<RepositoryInfo>) : RepositoryProvider {
        override fun getRepositories(): Collection<RepositoryInfo> =
            repositories
    }
}
