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

import com.reposilite.repository.specification.RepositoryRoutingSpecification
import io.javalin.http.HttpStatus.NOT_FOUND
import kong.unirest.core.Unirest.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RepositoryRoutingIntegrationTest : RepositoryRoutingSpecification() {

    @Test
    fun `should route repositories changed after startup`() {
        // given: a running server with a registered repository
        val provider = useRepositoryType("custom", "first")
        useServer { base ->
            assertThat(get("$base/first/pkg/file").asString().body).isEqualTo("first/pkg/file")

            // when: the provider replaces its repositories
            provider.currentRepositories = listOf(useRepository("second"))
            val oldRepository = get("$base/first/pkg/file").asEmpty()
            val newRepository = get("$base/second/pkg/file").asString()

            // then: routing uses the new repositories and preserves path parameters
            assertThat(oldRepository.status).isEqualTo(NOT_FOUND.code)
            assertThat(newRepository.body).isEqualTo("second/pkg/file")
        }
    }

    @Test
    fun `should reject conflicting repositories until the conflict is removed`() {
        // given: two providers exposing the same repository name
        useRepositoryType("custom", "shared")
        val other = useRepositoryType("other", "shared")
        useServer { base ->

            // when: the conflicting repository is requested
            val conflict = get("$base/shared/file").asEmpty()

            // then: neither provider is selected
            assertThat(conflict.status).isEqualTo(NOT_FOUND.code)

            // when: one provider removes its conflicting repository
            other.currentRepositories = emptyList()
            val response = get("$base/shared/file").asString()

            // then: the remaining repository is accessible without restarting
            assertThat(response.body).isEqualTo("shared/file")
        }
    }

    @Test
    fun `should reject duplicate names from the same provider`() {
        // given: one provider exposing a repository name twice
        useRepositoryType("custom", "shared", "shared")
        useServer { base ->

            // when: the ambiguous repository is requested
            val response = get("$base/shared/file").asEmpty()

            // then: routing does not select either repository
            assertThat(response.status).isEqualTo(NOT_FOUND.code)
        }
    }

    @Test
    fun `should keep running routes and providers on the same registration snapshot`() {
        // given: a server started with one provider
        useRepositoryType("custom", "downloads")
        useServer { base ->

            // when: another type registers a conflicting repository after startup
            useRepositoryType("late", "downloads")
            val response = get("$base/downloads/file").asString()

            // then: the new provider does not affect the running server's routes
            assertThat(response.body).isEqualTo("downloads/file")
        }
    }

}
