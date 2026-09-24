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

import com.reposilite.repository.api.RepositoryIdentity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class RepositoryIdentityTest {

    @ParameterizedTest
    @ValueSource(strings = ["", " ", " downloads", "downloads ", ".", "..", "../downloads", "down/loads", "down\\loads", "down\nloads"])
    fun `should reject repository names that cannot be routed safely`(name: String) {
        // given: a repository name that is not a valid path segment
        // when: an identity is created
        val result = RepositoryIdentity.create(name)

        // then: creation returns an error without throwing
        assertThat(result.error).contains("URL path segment")
    }

    @ParameterizedTest
    @ValueSource(strings = ["releases", "Maven-123", "NEGATIVE_CACHE", "repo.name"])
    fun `should preserve supported repository names`(name: String) {
        // given: a supported repository name
        // when: an identity is created
        val result = RepositoryIdentity.create(name)

        // then: the identity retains the name unchanged
        assertThat(result.get().name).isEqualTo(name)
    }
}
