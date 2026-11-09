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

package com.reposilite.maven

import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.application.MirroredRepositorySettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.repository.api.RepositoryVisibility.PUBLIC
import com.reposilite.storage.api.toLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError

internal class UpstreamFailureTest : MavenSpecification() {

    override fun repositories() = listOf(
        RepositorySettings("BROKEN", visibility = PUBLIC, proxied = listOf(
            MirroredRepositorySettings(reference = REMOTE_REPOSITORY_BROKEN)
        )),
        RepositorySettings("MISSING", visibility = PUBLIC, proxied = listOf(
            MirroredRepositorySettings(reference = REMOTE_REPOSITORY_WITH_WHITELIST)
        )),
        RepositorySettings("BROKEN-AND-MISSING", visibility = PUBLIC, proxied = listOf(
            MirroredRepositorySettings(reference = REMOTE_REPOSITORY_WITH_WHITELIST),
            MirroredRepositorySettings(reference = REMOTE_REPOSITORY_BROKEN),
        )),
    )

    private val gav = "com/example/lib/1.0.0/lib-1.0.0.jar".toLocation()

    @Test
    fun `should return upstream server error instead of local 404 when looking up details`() {
        // when: the only upstream fails
        val result = mavenFacade.findDetails(LookupRequest(UNAUTHORIZED, "BROKEN", gav))

        // then: the upstream error is returned
        assertThat(assertError(result).status).isEqualTo(502)
    }

    @Test
    fun `should return upstream server error instead of local 404 when looking up data`() {
        // when: the only upstream fails
        val result = mavenFacade.findData(LookupRequest(UNAUTHORIZED, "BROKEN", gav))

        // then: the upstream error is returned
        assertThat(assertError(result).status).isEqualTo(502)
    }

    @Test
    fun `should return server error when one upstream is missing the file and another fails`() {
        // when: one upstream doesn't have the file and the other fails
        val result = mavenFacade.findDetails(LookupRequest(UNAUTHORIZED, "BROKEN-AND-MISSING", gav))

        // then: the upstream error is returned
        assertThat(assertError(result).status).isEqualTo(502)
    }

    @Test
    fun `should still return 404 when no upstream has the file`() {
        // when: the only upstream doesn't have the file
        val result = mavenFacade.findDetails(LookupRequest(UNAUTHORIZED, "MISSING", gav))

        // then: it is reported as missing
        assertThat(assertError(result).status).isEqualTo(404)
    }

}
