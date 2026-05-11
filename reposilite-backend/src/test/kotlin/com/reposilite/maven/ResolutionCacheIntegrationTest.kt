/*
 * Copyright (c) 2026 dzikoysk
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

import com.reposilite.maven.RepositoryVisibility.PUBLIC
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.application.MirroredRepositorySettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.storage.api.toLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class ResolutionCacheIntegrationTest : MavenSpecification() {

    // The fake REMOTE_REPOSITORY_WITH_WHITELIST 404s anything not ending in "/allow", giving us a real upstream miss to cache.
    override fun repositories() = listOf(
        RepositorySettings(
            id = "CACHED",
            visibility = PUBLIC,
            storagePolicy = StoragePolicy.STRICT,
            resolutionCacheMaxEntries = 16,
            proxied = listOf(
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY_WITH_WHITELIST),
            ),
        ),
    )

    private fun upstreamMetadataUri(gav: String): String =
        "$REMOTE_REPOSITORY_WITH_WHITELIST/$gav/$METADATA_FILE"

    private fun upstreamFileUri(gav: String): String =
        "$REMOTE_REPOSITORY_WITH_WHITELIST/$gav"

    @Test
    fun `negative metadata result is cached and subsequent requests under the prefix short-circuit`() {
        // given: a GA whose metadata does not exist upstream
        val ga = "com/missing/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()

        // when: the metadata is requested twice
        val firstAttempt = mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata))
        val secondAttempt = mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata))

        // then: both surface 404 and the upstream was only probed once
        assertError(firstAttempt)
        assertError(secondAttempt)
        assertThat(remoteRequestsByUri[upstreamMetadataUri(ga)]?.get()).isEqualTo(1)

        // and: a JAR under the same prefix also short-circuits without hitting upstream
        val jarLookup = mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", "$ga/1.0/foo.jar".toLocation()))
        assertError(jarLookup)
        assertThat(remoteRequestsByUri[upstreamFileUri("$ga/1.0/foo.jar")]).isNull()
    }

    @Test
    fun `metadata resolved locally is recorded as Local so subsequent requests skip the upstream probe`() {
        // given: a metadata file already present in local storage
        val ga = "com/locally/present"
        addFileToRepository(FileSpec("CACHED", "/$ga/$METADATA_FILE", "<metadata/>"))

        // when: the metadata is requested
        val firstAttempt = mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", "$ga/$METADATA_FILE".toLocation()))

        // then: it resolves from local and the cache learns about it
        assertOk(firstAttempt)
        val origin = mavenFacade.getRepository("CACHED")!!.resolutionCache!!
            .lookup("$ga/$METADATA_FILE".toLocation(), authenticated = false)
        assertThat(origin).isEqualTo(ResolutionCache.Origin.Local)

        // and: no upstream call was made (and a second request also stays local)
        assertThat(remoteRequestsByUri[upstreamMetadataUri(ga)]).isNull()
        mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", "$ga/$METADATA_FILE".toLocation()))
        assertThat(remoteRequestsByUri[upstreamMetadataUri(ga)]).isNull()
    }

    @Test
    fun `local deployment under a cached prefix invalidates the entry`() {
        // given: a 404'd metadata that has been cached as negative
        val ga = "com/deployed/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata))
        assertThat(remoteRequestsByUri[upstreamMetadataUri(ga)]?.get()).isEqualTo(1)

        // when: a file is deployed under that prefix
        val deploy = mavenFacade.deployFile(
            DeployRequest(
                repository = mavenFacade.getRepository("CACHED")!!,
                gav = "$ga/1.0/foo.jar".toLocation(),
                by = "test",
                content = "payload".byteInputStream(),
                generateChecksums = false,
            )
        )
        assertOk(deploy)

        // then: a fresh request re-probes upstream (the entry was invalidated)
        mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata))
        assertThat(remoteRequestsByUri[upstreamMetadataUri(ga)]?.get()).isEqualTo(2)
    }
}
