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
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.SaveMetadataRequest
import com.reposilite.maven.application.MirroredRepositorySettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.storage.api.toLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class ResolutionCacheIntegrationTest : MavenSpecification() {

    // CACHED          — STRICT, non-storing whitelist mirror that 404s anything not ending in "/allow" → exercises Local / MissingMetadata paths.
    // MIRROR_STORE    — PRIORITIZE_UPSTREAM_METADATA, mirror with store=true, maxAge=60 → exercises the "we own a local copy now, cache as Local" path.
    // MIRROR_PIN      — PRIORITIZE_UPSTREAM_METADATA, mirror with store=false → exercises Remote(host) pinning.
    // MIRROR_REFRESH  — PRIORITIZE_UPSTREAM_METADATA, mirror with store=true, maxAge=0 → exercises stale-local refresh (cached Local must not override the policy).
    // MIRROR_FAIL     — PRIORITIZE_UPSTREAM_METADATA, whitelist mirror that always 404s, maxAge=0 → exercises the "remote 404, fall back to local" backup.
    // MULTI_MIRROR    — PRIORITIZE_UPSTREAM_METADATA, two mirrors (failing whitelist + working REMOTE_REPOSITORY) → exercises pinning effectiveness across reads.
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
        RepositorySettings(
            id = "MIRROR_STORE",
            visibility = PUBLIC,
            storagePolicy = StoragePolicy.PRIORITIZE_UPSTREAM_METADATA,
            metadataMaxAge = 60L,
            resolutionCacheMaxEntries = 16,
            proxied = listOf(
                MirroredRepositorySettings(
                    reference = REMOTE_REPOSITORY,
                    store = true,
                    authorization = REMOTE_AUTH,
                ),
            ),
        ),
        RepositorySettings(
            id = "MIRROR_PIN",
            visibility = PUBLIC,
            storagePolicy = StoragePolicy.PRIORITIZE_UPSTREAM_METADATA,
            metadataMaxAge = 60L,
            resolutionCacheMaxEntries = 16,
            proxied = listOf(
                MirroredRepositorySettings(
                    reference = REMOTE_REPOSITORY,
                    store = false,
                    authorization = REMOTE_AUTH,
                ),
            ),
        ),
        RepositorySettings(
            id = "MIRROR_REFRESH",
            visibility = PUBLIC,
            storagePolicy = StoragePolicy.PRIORITIZE_UPSTREAM_METADATA,
            metadataMaxAge = 0L,
            resolutionCacheMaxEntries = 16,
            proxied = listOf(
                MirroredRepositorySettings(
                    reference = REMOTE_REPOSITORY,
                    store = true,
                    authorization = REMOTE_AUTH,
                ),
            ),
        ),
        RepositorySettings(
            id = "MIRROR_FAIL",
            visibility = PUBLIC,
            storagePolicy = StoragePolicy.PRIORITIZE_UPSTREAM_METADATA,
            metadataMaxAge = 0L,
            resolutionCacheMaxEntries = 16,
            proxied = listOf(
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY_WITH_WHITELIST),
            ),
        ),
        RepositorySettings(
            id = "MULTI_MIRROR",
            visibility = PUBLIC,
            storagePolicy = StoragePolicy.PRIORITIZE_UPSTREAM_METADATA,
            metadataMaxAge = 60L,
            resolutionCacheMaxEntries = 16,
            proxied = listOf(
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY_WITH_WHITELIST),
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY, authorization = REMOTE_AUTH),
            ),
        ),
        RepositorySettings(
            id = "MIRROR_TRANSIENT",
            visibility = PUBLIC,
            storagePolicy = StoragePolicy.PRIORITIZE_UPSTREAM_METADATA,
            metadataMaxAge = 0L,
            resolutionCacheMaxEntries = 16,
            proxied = listOf(
                MirroredRepositorySettings(reference = REMOTE_REPOSITORY_BROKEN),
            ),
        ),
    )

    private fun upstreamMetadataUri(gav: String): String =
        "$REMOTE_REPOSITORY_WITH_WHITELIST/$gav/$METADATA_FILE"

    private fun upstreamFileUri(gav: String): String =
        "$REMOTE_REPOSITORY_WITH_WHITELIST/$gav"

    private fun storingMirrorUri(gav: String): String =
        "$REMOTE_REPOSITORY/$gav/$METADATA_FILE"

    @Test
    fun `negative metadata result is cached and short-circuits repeated reads of the same metadata`() {
        // given: a GA whose metadata does not exist upstream
        val ga = "com/missing/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()

        // when: the metadata is requested twice
        assertError(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata)))
        assertError(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata)))

        // then: the upstream was only probed once — Negative at the exact prefix short-circuits the second read
        assertThat(remoteRequestsByUri[upstreamMetadataUri(ga)]?.get()).isEqualTo(1)
    }

    @Test
    fun `metadata resolved locally is recorded as Local so subsequent requests skip the upstream probe`() {
        // given: a metadata file already present in local storage
        val ga = "com/locally/present"
        addFileToRepository(FileSpec("CACHED", "/$ga/$METADATA_FILE", "<metadata/>"))

        // when: the metadata is requested twice
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", "$ga/$METADATA_FILE".toLocation())))
        mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", "$ga/$METADATA_FILE".toLocation()))

        // then: the cache records Local and no upstream call was made
        val origin = mavenFacade.getRepository("CACHED")!!.resolutionCache!!
            .lookup("$ga/$METADATA_FILE".toLocation(), authenticated = false)
        assertThat(origin).isEqualTo(ResolutionCache.Origin.Local)
        assertThat(remoteRequestsByUri[upstreamMetadataUri(ga)]).isNull()
    }

    @Test
    fun `metadata deployment under a cached prefix invalidates the entry`() {
        // given: a Negative-cached prefix
        val ga = "com/deployed/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata))
        assertThat(remoteRequestsByUri[upstreamMetadataUri(ga)]?.get()).isEqualTo(1)

        // when: a metadata file is deployed under that prefix
        val deploy = mavenFacade.deployFile(
            DeployRequest(
                repository = mavenFacade.getRepository("CACHED")!!,
                gav = metadata,
                by = "test",
                content = "<metadata/>".byteInputStream(),
                generateChecksums = false,
            )
        )
        assertOk(deploy)

        // then: a fresh request re-probes and resolves locally
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata)))
        val origin = mavenFacade.getRepository("CACHED")!!.resolutionCache!!
            .lookup(metadata, authenticated = false)
        assertThat(origin).isEqualTo(ResolutionCache.Origin.Local)
    }

    @Test
    fun `non-metadata deployment leaves the cached entry intact`() {
        // given: a Negative-cached prefix
        val ga = "com/jaronly/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata))
        assertThat(remoteRequestsByUri[upstreamMetadataUri(ga)]?.get()).isEqualTo(1)

        // when: a JAR is deployed under that prefix
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

        // then: the cache is untouched and a subsequent metadata request stays short-circuited
        mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata))
        assertThat(remoteRequestsByUri[upstreamMetadataUri(ga)]?.get()).isEqualTo(1)
    }

    @Test
    fun `mirror with store=true caches metadata as Local since we own the copy after the first fetch`() {
        // given: a fresh fetch through a storing mirror has populated the cache as Local
        val ga = "com/storing/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MIRROR_STORE", metadata)))
        val origin = mavenFacade.getRepository("MIRROR_STORE")!!.resolutionCache!!
            .lookup(metadata, authenticated = false)
        assertThat(origin).isEqualTo(ResolutionCache.Origin.Local)
        val firstCallCount = remoteRequestsByUri[storingMirrorUri(ga)]!!.get()

        // when: the metadata is requested again within the maxAge window
        mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MIRROR_STORE", metadata))

        // then: no additional upstream call was issued
        assertThat(remoteRequestsByUri[storingMirrorUri(ga)]?.get()).isEqualTo(firstCallCount)
    }

    @Test
    fun `mirror with store=false pins to the winning host`() {
        // given: a PRIORITIZE_UPSTREAM_METADATA repo backed by a non-storing mirror
        val ga = "com/pinning/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()

        // when: the metadata is fetched from upstream
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MIRROR_PIN", metadata)))

        // then: the cache pins the prefix to the winning host
        val origin = mavenFacade.getRepository("MIRROR_PIN")!!.resolutionCache!!
            .lookup(metadata, authenticated = false)
        assertThat(origin).isEqualTo(ResolutionCache.Origin.Remote(REMOTE_REPOSITORY))
    }

    @Test
    fun `saveMetadata invalidates the cached entry for the prefix`() {
        // given: a prefix cached as Negative
        val ga = "com/savemetadata/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata))
        val cache = mavenFacade.getRepository("CACHED")!!.resolutionCache!!
        assertThat(cache.lookup(metadata, authenticated = false)).isEqualTo(ResolutionCache.Origin.MissingMetadata)

        // when: a server-side metadata write happens (saveMetadata bypasses deployFile)
        val saved = mavenFacade.saveMetadata(
            SaveMetadataRequest(
                repository = mavenFacade.getRepository("CACHED")!!,
                gav = ga.toLocation(),
                metadata = Metadata()
            )
        )
        assertOk(saved)

        // then: the Negative entry is cleared and a subsequent read promotes the prefix to Local
        assertThat(cache.lookup(metadata, authenticated = false)).isNull()
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata)))
        assertThat(cache.lookup(metadata, authenticated = false)).isEqualTo(ResolutionCache.Origin.Local)
    }

    @Test
    fun `cached Local does not override PRIORITIZE_UPSTREAM_METADATA when metadata is stale`() {
        // given: a fresh fetch under maxAge=0 (always-stale) has populated the cache as Local
        val ga = "com/refresh/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        // findFile returns a LockedFilterInputStream holding a READ lock; leaving it open deadlocks the next putFile.
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MIRROR_REFRESH", metadata))).content.close()
        val origin = mavenFacade.getRepository("MIRROR_REFRESH")!!.resolutionCache!!
            .lookup(metadata, authenticated = false)
        assertThat(origin).isEqualTo(ResolutionCache.Origin.Local)
        val callsAfterFirst = remoteRequestsByUri[storingMirrorUri(ga)]!!.get()

        // when: the metadata is requested again
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MIRROR_REFRESH", metadata))).content.close()

        // then: another upstream call was issued — the policy refresh wins over the cached hint
        assertThat(remoteRequestsByUri[storingMirrorUri(ga)]!!.get()).isGreaterThan(callsAfterFirst)
    }

    @Test
    fun `local backup serves metadata when upstream refresh fails under PRIORITIZE_UPSTREAM_METADATA`() {
        // given: a maxAge=0 repo with a 404-only mirror and a metadata file already present locally
        val ga = "com/backup/foo"
        val metadata = "$ga/$METADATA_FILE"
        addFileToRepository(FileSpec("MIRROR_FAIL", "/$metadata", "<metadata/>"))

        // when: the metadata is requested
        val result = mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MIRROR_FAIL", metadata.toLocation()))

        // then: the upstream was probed, the local copy served, and the cache learned Local
        assertOk(result)
        assertThat(remoteRequestsByUri["$REMOTE_REPOSITORY_WITH_WHITELIST/$metadata"]?.get()).isGreaterThanOrEqualTo(1)
        val origin = mavenFacade.getRepository("MIRROR_FAIL")!!.resolutionCache!!
            .lookup(metadata.toLocation(), authenticated = false)
        assertThat(origin).isEqualTo(ResolutionCache.Origin.Local)
    }

    @Test
    fun `directly-uploaded JAR under a Negative-cached prefix is still served`() {
        // given: a Negative-cached GA prefix and a JAR uploaded directly (no metadata)
        val ga = "com/spring-like/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        val jar = "$ga/1.0.0/foo-1.0.0.jar".toLocation()
        assertError(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata)))
        val cache = mavenFacade.getRepository("CACHED")!!.resolutionCache!!
        assertThat(cache.lookup(metadata, authenticated = false)).isEqualTo(ResolutionCache.Origin.MissingMetadata)
        addFileToRepository(FileSpec("CACHED", "/$jar", "jarbytes"))

        // when: the JAR is requested
        val result = mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", jar))

        // then: it is served
        assertOk(result).content.close()
    }

    @Test
    fun `metadata deploy at the same prefix invalidates a Negative-cached entry`() {
        // given: a Negative entry recorded at the artifact-level metadata prefix
        val ga = "com/redeploy/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        assertError(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata)))
        val cache = mavenFacade.getRepository("CACHED")!!.resolutionCache!!
        assertThat(cache.lookup(metadata, authenticated = false)).isEqualTo(ResolutionCache.Origin.MissingMetadata)

        // when: the metadata itself is deployed at the same prefix
        val deploy = mavenFacade.deployFile(
            DeployRequest(
                repository = mavenFacade.getRepository("CACHED")!!,
                gav = metadata,
                by = "test",
                content = "<metadata/>".byteInputStream(),
                generateChecksums = false,
            )
        )
        assertOk(deploy)

        // then: the Negative entry at the exact prefix is cleared
        assertThat(cache.lookup(metadata, authenticated = false)).isNull()
    }

    @Test
    fun `checksum sibling deploy does not invalidate the cache`() {
        // given: a metadata prefix cached as Local
        val ga = "com/checksum/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        addFileToRepository(FileSpec("CACHED", "/$ga/$METADATA_FILE", "<metadata/>"))
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", metadata))).content.close()
        val cache = mavenFacade.getRepository("CACHED")!!.resolutionCache!!
        assertThat(cache.lookup(metadata, authenticated = false)).isEqualTo(ResolutionCache.Origin.Local)

        // when: a checksum sibling of the metadata file is deployed
        val deploy = mavenFacade.deployFile(
            DeployRequest(
                repository = mavenFacade.getRepository("CACHED")!!,
                gav = "$ga/$METADATA_FILE.sha1".toLocation(),
                by = "test",
                content = "deadbeef".byteInputStream(),
                generateChecksums = false,
            )
        )
        assertOk(deploy)

        // then: the Local entry is preserved
        assertThat(cache.lookup(metadata, authenticated = false)).isEqualTo(ResolutionCache.Origin.Local)
    }

    @Test
    fun `pinning narrows subsequent requests to the winning host across multi-mirror setups`() {
        // given: a repo with a failing mirror first and a serving mirror second; the first request has cached the winner
        val ga = "com/multimirror/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        val h1Uri = "$REMOTE_REPOSITORY_WITH_WHITELIST/$ga/$METADATA_FILE"
        val h2Uri = "$REMOTE_REPOSITORY/$ga/$METADATA_FILE"
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MULTI_MIRROR", metadata)))
        val h1AfterFirst = remoteRequestsByUri[h1Uri]?.get() ?: 0
        val h2AfterFirst = remoteRequestsByUri[h2Uri]?.get() ?: 0
        assertThat(h1AfterFirst).isGreaterThanOrEqualTo(1)
        assertThat(h2AfterFirst).isGreaterThanOrEqualTo(1)
        assertThat(mavenFacade.getRepository("MULTI_MIRROR")!!.resolutionCache!!.lookup(metadata, authenticated = false))
            .isEqualTo(ResolutionCache.Origin.Remote(REMOTE_REPOSITORY))

        // when: the metadata is requested again
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MULTI_MIRROR", metadata)))

        // then: only the pinned host is hit
        assertThat(remoteRequestsByUri[h1Uri]?.get() ?: 0).isEqualTo(h1AfterFirst)
        assertThat(remoteRequestsByUri[h2Uri]?.get() ?: 0).isGreaterThan(h2AfterFirst)
    }

    @Test
    fun `child deploy must not invalidate cached ancestor prefixes`() {
        // given: a group-level metadata read populates the cache at the ancestor prefix `org/example`
        // (group-level metadata.xml is the real Maven concept for plugin discovery)
        addFileToRepository(FileSpec("CACHED", "/org/example/$METADATA_FILE", "<metadata/>"))
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "CACHED", "org/example/$METADATA_FILE".toLocation())))
            .content.close()
        val cache = mavenFacade.getRepository("CACHED")!!.resolutionCache!!
        assertThat(cache.lookup("org/example/$METADATA_FILE".toLocation(), authenticated = false))
            .isEqualTo(ResolutionCache.Origin.Local)

        // when: an unrelated nested POM is deployed deep under that prefix
        val deploy = mavenFacade.deployFile(
            DeployRequest(
                repository = mavenFacade.getRepository("CACHED")!!,
                gav = "org/example/foo/1.0/foo-1.0.pom".toLocation(),
                by = "test",
                content = "<project/>".byteInputStream(),
                generateChecksums = false,
            )
        )
        assertOk(deploy)

        // then: the ancestor `org/example` entry survives — it carries a legitimate routing decision
        // unrelated to the nested artifact, and invalidating it forces a re-probe of every group-level read.
        assertThat(cache.lookup("org/example/$METADATA_FILE".toLocation(), authenticated = false))
            .isEqualTo(ResolutionCache.Origin.Local)
    }

    @Test
    fun `negative cached at an ancestor prefix must not swallow newly-appeared lower-prefix metadata`() {
        // given: a tooling probe at the group-level metadata records Negative at `com/foo`
        // (this is the case where a tool like versions:display-dependency-updates fires once)
        val groupMetadata = "com/foo/$METADATA_FILE".toLocation()
        assertError(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MIRROR_FAIL", groupMetadata)))
        val cache = mavenFacade.getRepository("MIRROR_FAIL")!!.resolutionCache!!
        assertThat(cache.lookup(groupMetadata, authenticated = false))
            .isEqualTo(ResolutionCache.Origin.MissingMetadata)

        // and: a metadata file appears at a lower prefix via a path that bypasses deployFile
        // (S3 copy, plugin-installed writer, manual fs write, partial-publish race — all real)
        val artifactMetadata = "com/foo/bar/$METADATA_FILE"
        addFileToRepository(FileSpec("MIRROR_FAIL", "/$artifactMetadata", "<metadata/>"))

        // when: the lower-prefix metadata is requested
        val result = mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MIRROR_FAIL", artifactMetadata.toLocation()))

        // then: it must be served from local — the ancestor's Negative says nothing about descendants
        // that may have been populated independently.
        assertOk(result).content.close()
    }

    @Test
    fun `transient upstream failure with local fallback does not poison the cache as Local`() {
        // given: PRIORITIZE_UPSTREAM_METADATA with maxAge=0 so every request takes the remote-first path,
        // and a mirror that always responds with a 502 (non-404) — i.e. transient failure, not a definitive miss.
        // Local has a pre-populated metadata that can serve as backup.
        val ga = "com/transient/foo"
        val metadata = "$ga/$METADATA_FILE".toLocation()
        addFileToRepository(FileSpec("MIRROR_TRANSIENT", "/$metadata", "<metadata/>"))

        // when: the metadata is requested — remote fails with 502, local serves as backup
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "MIRROR_TRANSIENT", metadata))).content.close()

        // then: the cache must NOT record this prefix as Local — the upstream failure was transient
        // and trusting "Local" here would let a future read silently keep serving stale through an outage
        // (per the explicit "don't poison cache" intent at recordTo's else branch).
        val cache = mavenFacade.getRepository("MIRROR_TRANSIENT")!!.resolutionCache!!
        assertThat(cache.lookup(metadata, authenticated = false)).isNull()
    }
}
