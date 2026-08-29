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

import com.reposilite.maven.ResolutionCache.State.MirrorsMissing
import com.reposilite.maven.ResolutionCache.State.PinnedMirror
import com.reposilite.maven.ResolutionCacheLevel.NEGATIVE_CACHING
import com.reposilite.maven.ResolutionCacheLevel.PINNING
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.storage.api.toLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ResolutionCacheTest {

    @Test
    fun `pin applies only to explicit version and GA scopes`() {
        // given: distinct pins discovered through GA and version metadata
        val cache = ResolutionCache(4, PINNING)
        cache.recordPinnedMirror("org/example/foo/$METADATA_FILE".toLocation(), false, "central")
        cache.recordPinnedMirror("org/example/foo/1.0/$METADATA_FILE".toLocation(), false, "snapshots")

        // when: artifacts within and outside those scopes are looked up
        val version = cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), false)
        val artifact = cache.lookup("org/example/foo/2.0/foo.jar".toLocation(), false)
        val sibling = cache.lookup("org/example/bar/1.0/bar.jar".toLocation(), false)

        // then: the closest explicit pin wins without inheriting from arbitrary group ancestors
        assertThat(version).isEqualTo(PinnedMirror("snapshots"))
        assertThat(artifact).isEqualTo(PinnedMirror("central"))
        assertThat(sibling).isNull()
    }

    @Test
    fun `negative caching retains exact mirror misses`() {
        // given: negative and pinning-only caches
        val negative = ResolutionCache(4, NEGATIVE_CACHING)
        val pinning = ResolutionCache(4, PINNING)
        val metadata = "org/example/foo/$METADATA_FILE".toLocation()

        // when: the same mirror miss is recorded in both caches
        negative.recordMirrorsMissing(metadata, false)
        pinning.recordMirrorsMissing(metadata, false)

        // then: only the negative cache retains the exact path
        assertThat(negative.lookup(metadata, false)).isEqualTo(MirrorsMissing)
        assertThat(negative.lookup("org/example/foo/foo.pom".toLocation(), false)).isNull()
        assertThat(pinning.lookup(metadata, false)).isNull()
    }

    @Test
    fun `cache remains bounded`() {
        // given: a cache at capacity
        val cache = ResolutionCache(2, NEGATIVE_CACHING)
        cache.recordPinnedMirror("a/$METADATA_FILE".toLocation(), false, "host-a")
        cache.recordMirrorsMissing("b/$METADATA_FILE".toLocation(), false)

        // when: another entry is recorded
        cache.recordPinnedMirror("c/$METADATA_FILE".toLocation(), false, "host-c")

        // then: Caffeine evicts an entry to preserve the configured bound
        assertThat(cache.size()).isEqualTo(2)
    }

    @Test
    fun `authentication buckets are isolated and exact entries can be invalidated`() {
        // given: different states for anonymous and authenticated requests
        val cache = ResolutionCache(4, NEGATIVE_CACHING)
        val metadata = "org/example/foo/$METADATA_FILE".toLocation()
        cache.recordPinnedMirror(metadata, false, "public")
        cache.recordPinnedMirror(metadata, true, "private")
        cache.recordMirrorsMissing(metadata, false)
        cache.recordMirrorsMissing(metadata, true)

        // when: the exact metadata path is invalidated
        cache.invalidate(metadata)

        // then: both misses are removed without removing either routing pin
        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), false)).isEqualTo(PinnedMirror("public"))
        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), true)).isEqualTo(PinnedMirror("private"))
    }

}
