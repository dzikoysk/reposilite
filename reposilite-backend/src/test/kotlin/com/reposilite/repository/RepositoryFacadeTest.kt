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

import com.reposilite.repository.specification.RepositorySpecification
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.web.api.ReposiliteRoute
import io.javalin.community.routing.Route.BEFORE
import io.javalin.community.routing.Route.GET
import io.javalin.http.HttpStatus.BAD_REQUEST
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class RepositoryFacadeTest : RepositorySpecification() {

    @Test
    fun `should reject blank repository type`() {
        // given: a provider with a valid repository
        val provider = useRepositoryProvider("downloads")

        // when: registration uses a blank type
        val result = repositoryFacade.register(" ", useRoutes(), provider)

        // then: the error is returned without adding a registration
        assertThat(assertError(result)).contains("cannot be blank")
        assertThat(repositoryFacade.getRegistrations()).isEmpty()
    }

    @Test
    fun `should reject duplicate repository type`() {
        // given: an already registered repository type
        val provider = useRepositoryProvider("first")
        assertOk(repositoryFacade.register("custom", useRoutes(), provider))

        // when: another provider registers the same type
        val result = repositoryFacade.register("custom", useRoutes(), useRepositoryProvider("second"))

        // then: the original provider is retained
        assertThat(assertError(result)).contains("already registered")
        assertThat(repositoryFacade.getRegistrations().getValue("custom").provider).isSameAs(provider)
    }

    @Test
    fun `should reject routes outside repository gateway`() {
        // given: a route without the repository path parameter
        val routes = useRoutes(ReposiliteRoute<Unit>("/cargo/<crate>", GET) {})
        val provider = useRepositoryProvider("cargo")

        // when: the invalid routes are registered
        val result = repositoryFacade.register("cargo", routes, provider)

        // then: the registration is rejected
        assertThat(assertError(result)).contains("/{repository}")
        assertThat(repositoryFacade.getRegistrations()).isEmpty()

        // when: the type is registered with valid routes
        val retry = repositoryFacade.register("cargo", useRoutes(), provider)

        // then: the rejected registration does not reserve the type
        assertOk(retry)
    }

    @Test
    fun `should reject filter routes`() {
        // given: a filter instead of an HTTP endpoint
        val routes = useRoutes(ReposiliteRoute<Unit>("/{repository}/<path>", BEFORE) {})

        // when: the filter is registered as a repository route
        val result = repositoryFacade.register("cargo", routes, useRepositoryProvider("cargo"))

        // then: the error is returned without adding a registration
        assertThat(assertError(result)).contains("HTTP methods only")
        assertThat(repositoryFacade.getRegistrations()).isEmpty()
    }

    @Test
    fun `should reject current directory path before checking access`() {
        // given: a repository and a token with permission to modify its files
        val repository = useRepository("cargo")
        val token = createToken("manager").accessToken.identifier
        accessTokenFacade.addPermission(token, MANAGER)
        assertThat(repositoryFacade.canModifyResource(token, repository, Location.of("file"))).isTrue

        // when: the current directory path is checked
        val currentDirectory = Location.of(".")
        val access = repositoryFacade.canAccessResource(token, repository, currentDirectory)
        val modification = repositoryFacade.canModifyResource(token, repository, currentDirectory)

        // then: the path is rejected despite the token's permissions
        assertThat(assertError(access).status).isEqualTo(BAD_REQUEST.code)
        assertThat(modification).isFalse
    }

}
