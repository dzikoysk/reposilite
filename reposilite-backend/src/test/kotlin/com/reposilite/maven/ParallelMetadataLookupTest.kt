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
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.application.MirroredRepositorySettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.storage.api.toLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit.SECONDS

internal class ParallelMetadataLookupTest : MavenSpecification() {

    private val metadata = "com/parallel/example/$METADATA_FILE"
    private val concurrentRequests = CyclicBarrier(2)

    override fun repositories() = listOf(
        RepositorySettings(
            id = "PARALLEL",
            parallelMetadataLookup = true,
            resolutionCacheMaxEntries = 16,
            proxied = listOf(
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY_WITH_WHITELIST),
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY, authorization = REMOTE_AUTH, store = true),
            ),
        )
    )

    override fun beforeRemoteHead(uri: String) {
        if (uri.endsWith(metadata)) {
            concurrentRequests.await(1, SECONDS)
        }
    }

    @Test
    fun `should probe metadata concurrently and route artifacts through the winner`() {
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "PARALLEL", metadata.toLocation()))).content.close()

        val artifact = "com/parallel/example/1.0/example-1.0.jar"
        assertOk(mavenFacade.findDetails(LookupRequest(UNAUTHORIZED, "PARALLEL", artifact.toLocation())))
        assertThat(remoteRequestsByUri["$REMOTE_REPOSITORY_WITH_WHITELIST/$artifact"]).isNull()
    }

}
