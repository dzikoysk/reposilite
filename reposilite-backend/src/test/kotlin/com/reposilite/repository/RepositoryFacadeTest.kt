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
import com.reposilite.repository.api.RepositoryAccessMode.PUBLIC
import com.reposilite.repository.api.RepositoryInfo
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
    fun `should resolve repository type by name`() {
        // given: two types with distinct repositories
        register("maven", "releases")
        register("custom", "downloads")

        // when: the repository type is resolved by name
        val types = facade.findRepositoryTypes("downloads")

        // then: the matching type is returned
        assertThat(types).containsExactly("custom")
    }

    @Test
    fun `should reflect repositories changed by registration`() {
        // given: registration backed by a repository reference
        val repositories = mutableReference<Collection<RepositoryInfo>>(listOf(repository("first")))
        facade.register("custom", emptyRoutes, repositories)

        // when: configuration replaces its repositories
        repositories.update(listOf(repository("second")))

        // then: facade queries the reference instead of serving a stale index
        assertThat(facade.findRepositoryTypes("first")).isEmpty()
        assertThat(facade.findRepositoryTypes("second")).containsExactly("custom")
    }

    @Test
    fun `should reject duplicate repository type`() {
        // given: an already registered repository type
        register("custom", "first")

        // when & then: another registration of the same type is rejected
        assertThatIllegalArgumentException()
            .isThrownBy { register("custom", "second") }
            .withMessageContaining("already registered")
    }

    @Test
    fun `should report all types sharing a repository name`() {
        // given: two types sharing a repository name
        val customRepositories = mutableReference<Collection<RepositoryInfo>>(listOf(repository("shared")))
        register("maven", "shared")
        facade.register("custom", emptyRoutes, customRepositories)

        // when: repositories are queried while the name is ambiguous
        val types = facade.findRepositoryTypes("shared")

        // then: both types are returned so the gateway can reject the ambiguous name
        assertThat(types).containsExactly("maven", "custom")

        // when: one type removes its conflicting repository
        customRepositories.update(emptyList())

        // then: the remaining repository can be routed unambiguously
        assertThat(facade.findRepositoryTypes("shared")).containsExactly("maven")
    }

    @Test
    fun `should preserve duplicate repository names within a type`() {
        // given: a type exposing two repositories with the same name
        register("custom", "shared", "shared")

        // when: the repository type is resolved
        val types = facade.findRepositoryTypes("shared")

        // then: the duplicate remains ambiguous to the gateway
        assertThat(types).containsExactly("custom", "custom")
    }

    @Test
    fun `should reject types registered after routing is sealed`() {
        // given: sealed repository routing
        register("maven", "releases")
        facade.validateAndSeal()

        // when & then: a late repository type registration is rejected
        assertThatIllegalStateException()
            .isThrownBy { register("cargo", "cargo-releases") }
            .withMessageContaining("before the HTTP server starts")
    }

    @Test
    fun `should reject repository names that cannot be routed safely`() {
        // given: a repository name containing a path operator
        val repositoryName = "../downloads"

        // when & then: the invalid repository name is rejected
        assertThatIllegalArgumentException()
            .isThrownBy { facade.validateRepositoryName(repositoryName) }
            .withMessageContaining("URL path segment")
    }

    @Test
    fun `should reject routes outside repository gateway`() {
        // given: a repository route without the repository path parameter
        val invalidRoutes = object : ReposiliteRoutes() {
            override val routes = routes(ReposiliteRoute<Unit>("/cargo/<crate>", GET) {})
        }
        facade.register("cargo", invalidRoutes, listOf(repository("cargo")).toReference())

        // when & then: repository routing validation rejects the route
        assertThatIllegalArgumentException()
            .isThrownBy { facade.validateAndSeal() }
            .withMessageContaining("/{repository}")
    }

    @Test
    fun `should reject filter routes`() {
        // given: a repository registration containing a filter route
        val invalidRoutes = object : ReposiliteRoutes() {
            override val routes = routes(ReposiliteRoute<Unit>("/{repository}/<path>", BEFORE) {})
        }
        facade.register("cargo", invalidRoutes, listOf(repository("cargo")).toReference())

        // when & then: repository routing validation rejects the filter
        assertThatIllegalArgumentException()
            .isThrownBy { facade.validateAndSeal() }
            .withMessageContaining("HTTP methods only")
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

    private fun register(type: String, vararg names: String) {
        facade.register(type, emptyRoutes, names.map { repository(it) }.toReference())
    }

    private fun repository(id: String): RepositoryInfo =
        RepositoryInfo(id, PUBLIC)
}
