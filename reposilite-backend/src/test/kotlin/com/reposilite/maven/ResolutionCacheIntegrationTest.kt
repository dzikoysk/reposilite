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

import com.reposilite.maven.RepositoryVisibility.PUBLIC
import com.reposilite.maven.ResolutionCache.State.MirrorsMissing
import com.reposilite.maven.ResolutionCache.State.PinnedMirror
import com.reposilite.maven.ResolutionCacheLevel.PINNING
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
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit.MINUTES

internal class ResolutionCacheIntegrationTest : MavenSpecification() {

    override fun repositories() = listOf(
        RepositorySettings(
            id = "NEGATIVE_CACHE",
            visibility = PUBLIC,
            storagePolicy = StoragePolicy.STRICT,
            metadataMaxAge = 60,
            resolutionCacheMaxEntries = 16,
            proxied = listOf(MirroredRepositorySettings(reference = REMOTE_REPOSITORY_WITH_WHITELIST)),
        ),
        RepositorySettings(
            id = "PINNING_ONLY",
            visibility = PUBLIC,
            storagePolicy = StoragePolicy.STRICT,
            metadataMaxAge = 60,
            resolutionCacheMaxEntries = 16,
            resolutionCacheLevel = PINNING,
            proxied = listOf(MirroredRepositorySettings(reference = REMOTE_REPOSITORY_WITH_WHITELIST)),
        ),
        RepositorySettings(
            id = "MULTI_MIRROR",
            visibility = PUBLIC,
            metadataMaxAge = 60,
            resolutionCacheMaxEntries = 16,
            resolutionCacheLevel = PINNING,
            proxied = listOf(
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY_WITH_WHITELIST),
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY, authorization = REMOTE_AUTH),
            ),
        ),
        RepositorySettings(
            id = "PARALLEL",
            visibility = PUBLIC,
            metadataMaxAge = 60,
            resolutionCacheMaxEntries = 16,
            resolutionCacheLevel = PINNING,
            parallelMetadataLookup = true,
            proxied = listOf(
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY_WITH_WHITELIST),
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY, authorization = REMOTE_AUTH, store = true),
            ),
        ),
    )

    @Test
    fun `negative cache retains an exact mirror miss`() {
        // given: metadata that does not exist in the negative-cache repository
        val metadata = "com/missing/foo/$METADATA_FILE".toLocation()

        // when: the same metadata is requested twice
        assertError(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "NEGATIVE_CACHE", metadata)))
        assertError(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "NEGATIVE_CACHE", metadata)))

        // then: the remote is probed once and the exact miss is cached
        assertThat(remoteRequestsByUri["$REMOTE_REPOSITORY_WITH_WHITELIST/$metadata"]?.get()).isEqualTo(1)
        assertThat(mavenFacade.getRepository("NEGATIVE_CACHE")!!.resolutionCache!!.lookup(metadata, false)).isEqualTo(MirrorsMissing)
    }

    @Test
    fun `pinning level does not cache metadata misses`() {
        // given: metadata that does not exist in a pinning-only repository
        val metadata = "com/missing/pinning/$METADATA_FILE".toLocation()

        // when: the same metadata is requested twice
        assertError(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "PINNING_ONLY", metadata)))
        assertError(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "PINNING_ONLY", metadata)))

        // then: both requests probe the remote and no state is retained
        assertThat(remoteRequestsByUri["$REMOTE_REPOSITORY_WITH_WHITELIST/$metadata"]?.get()).isEqualTo(2)
        assertThat(mavenFacade.getRepository("PINNING_ONLY")!!.resolutionCache!!.lookup(metadata, false)).isNull()
    }

    @Test
    fun `cached mirror miss does not hide locally deployed metadata`() {
        // given: metadata deployed after an exact mirror miss was cached
        val metadata = "com/deployed/foo/$METADATA_FILE".toLocation()
        assertError(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "NEGATIVE_CACHE", metadata)))
        val repository = mavenFacade.getRepository("NEGATIVE_CACHE")!!
        val cache = repository.resolutionCache!!
        assertOk(mavenFacade.deployFile(DeployRequest(
            repository = repository,
            gav = metadata,
            by = "test",
            content = "<metadata/>".byteInputStream(),
            generateChecksums = false,
        )))
        cache.recordMirrorsMissing(metadata, false)

        // when: metadata is requested while the stale remote miss is still cached
        val result = mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "NEGATIVE_CACHE", metadata))

        // then: local storage remains authoritative
        assertOk(result).content.close()
    }

    @Test
    fun `cached mirror is tried first on subsequent requests`() {
        // given: metadata resolved by the second mirror
        val metadata = "com/pinned/foo/$METADATA_FILE".toLocation()
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MULTI_MIRROR", metadata))).content.close()
        val firstMirror = "$REMOTE_REPOSITORY_WITH_WHITELIST/$metadata"
        val secondMirror = "$REMOTE_REPOSITORY/$metadata"
        val firstMirrorRequests = remoteRequestsByUri[firstMirror]?.get()
        val secondMirrorRequests = remoteRequestsByUri[secondMirror]?.get() ?: 0

        // when: the metadata is requested again
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MULTI_MIRROR", metadata))).content.close()

        // then: the pinned mirror serves it without probing the first mirror again
        assertThat(remoteRequestsByUri[firstMirror]?.get()).isEqualTo(firstMirrorRequests)
        assertThat(remoteRequestsByUri[secondMirror]?.get()).isGreaterThan(secondMirrorRequests)
        assertThat(mavenFacade.getRepository("MULTI_MIRROR")!!.resolutionCache!!.lookup(metadata, false))
            .isEqualTo(PinnedMirror(REMOTE_REPOSITORY))
    }

    @Test
    fun `mirror pin does not override locally deployed metadata`() {
        // given: metadata pinned to a mirror and then deployed locally
        val metadata = "com/pinned/local/$METADATA_FILE".toLocation()
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MULTI_MIRROR", metadata))).content.close()
        val repository = mavenFacade.getRepository("MULTI_MIRROR")!!
        assertOk(mavenFacade.deployFile(DeployRequest(
            repository = repository,
            gav = metadata,
            by = "test",
            content = "local".byteInputStream(),
            generateChecksums = false,
        )))
        val remote = "$REMOTE_REPOSITORY/$metadata"
        val remoteRequests = remoteRequestsByUri[remote]?.get()

        // when: the metadata is requested again
        val result = assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MULTI_MIRROR", metadata)))

        // then: local storage remains authoritative and the pin is only used for remote routing
        assertThat(result.content.use { it.readBytes().decodeToString() }).isEqualTo("local")
        assertThat(remoteRequestsByUri[remote]?.get()).isEqualTo(remoteRequests)
    }

    @Test
    fun `stale mirror pin falls back to remaining mirrors`() {
        // given: metadata incorrectly pinned to a mirror that does not contain it
        val metadata = "com/stale/foo/$METADATA_FILE".toLocation()
        val repository = mavenFacade.getRepository("MULTI_MIRROR")!!
        val cache = repository.resolutionCache!!
        cache.recordPinnedMirror(metadata, false, REMOTE_REPOSITORY_WITH_WHITELIST)

        // when: the metadata is requested
        val result = mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MULTI_MIRROR", metadata))

        // then: resolution continues through the remaining mirrors and updates the pin
        assertOk(result).content.close()
        assertThat(remoteRequestsByUri["$REMOTE_REPOSITORY_WITH_WHITELIST/$metadata"]?.get()).isGreaterThan(0)
        assertThat(remoteRequestsByUri["$REMOTE_REPOSITORY/$metadata"]?.get()).isGreaterThan(0)
        assertThat(cache.lookup(metadata, false)).isEqualTo(PinnedMirror(REMOTE_REPOSITORY))
    }

    @Test
    fun `parallel metadata lookup retains the winner after a local metadata hit`() {
        // given: metadata lookup synchronized across both configured mirrors
        val metadata = "com/parallel/example/$METADATA_FILE"
        val concurrentRequests = CyclicBarrier(2)
        beforeRemoteHead = { uri ->
            if (uri.endsWith(metadata)) {
                concurrentRequests.await(1, MINUTES)
            }
        }

        // when: metadata is fetched, served locally, and followed by an artifact lookup
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "PARALLEL", metadata.toLocation()))).content.close()
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "PARALLEL", metadata.toLocation()))).content.close()
        val artifact = "com/parallel/example/1.0/example-1.0.jar"
        assertOk(mavenFacade.findDetails(LookupRequest(UNAUTHORIZED, "PARALLEL", artifact.toLocation())))

        // then: the artifact is routed directly through the metadata winner
        assertThat(remoteRequestsByUri["$REMOTE_REPOSITORY_WITH_WHITELIST/$artifact"]).isNull()
    }

}
