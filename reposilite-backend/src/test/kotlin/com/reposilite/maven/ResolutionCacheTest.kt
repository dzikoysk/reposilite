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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset.UTC

internal class ResolutionCacheTest {

    @Test
    fun `pin applies only to explicit version and GA scopes`() {
        // given: distinct pins discovered through GA and version metadata
        val cache = ResolutionCache(4, PINNING, 0)
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
    fun `negative caching stores exact mirror misses until metadata max age expires`() {
        // given: a negative cache with a controllable clock
        val clock = TestClock()
        val cache = ResolutionCache(4, NEGATIVE_CACHING, 60, clock)
        val metadata = "org/example/foo/$METADATA_FILE".toLocation()
        cache.recordMirrorsMissing(metadata, false)

        // when: the exact path, a sibling, and the expired path are looked up
        val exact = cache.lookup(metadata, false)
        val sibling = cache.lookup("org/example/foo/foo.pom".toLocation(), false)
        clock.now = clock.now.plusSeconds(60)
        val expired = cache.lookup(metadata, false)

        // then: only the unexpired exact metadata path fails fast
        assertThat(exact).isEqualTo(MirrorsMissing)
        assertThat(sibling).isNull()
        assertThat(expired).isNull()
    }

    @Test
    fun `pinning level and zero max age do not retain mirror misses`() {
        // given: caches that do not permit a useful negative entry
        val pinning = ResolutionCache(2, PINNING, 60)
        val alwaysRefresh = ResolutionCache(2, NEGATIVE_CACHING, 0)
        val metadata = "org/example/foo/$METADATA_FILE".toLocation()

        // when: the mirror miss is recorded
        pinning.recordMirrorsMissing(metadata, false)
        alwaysRefresh.recordMirrorsMissing(metadata, false)

        // then: neither cache retains the miss
        assertThat(pinning.size()).isZero()
        assertThat(alwaysRefresh.size()).isZero()
    }

    @Test
    fun `mirror miss entries cannot displace mirror pins`() {
        // given: a cache filled with mirror pins
        val cache = ResolutionCache(2, NEGATIVE_CACHING, 60)
        cache.recordPinnedMirror("a/$METADATA_FILE".toLocation(), false, "host-a")
        cache.recordPinnedMirror("b/$METADATA_FILE".toLocation(), false, "host-b")

        // when: a mirror miss is recorded at capacity
        cache.recordMirrorsMissing("missing/$METADATA_FILE".toLocation(), false)

        // then: both pins remain and the lower-priority miss is rejected
        assertThat(cache.lookup("a/1/a.jar".toLocation(), false)).isEqualTo(PinnedMirror("host-a"))
        assertThat(cache.lookup("b/1/b.jar".toLocation(), false)).isEqualTo(PinnedMirror("host-b"))
        assertThat(cache.lookup("missing/$METADATA_FILE".toLocation(), false)).isNull()
    }

    @Test
    fun `mirror pins replace mirror misses at capacity`() {
        // given: a cache filled with exact mirror misses
        val cache = ResolutionCache(2, NEGATIVE_CACHING, 60)
        cache.recordMirrorsMissing("a/$METADATA_FILE".toLocation(), false)
        cache.recordMirrorsMissing("b/$METADATA_FILE".toLocation(), false)

        // when: a mirror is pinned at capacity
        cache.recordPinnedMirror("c/$METADATA_FILE".toLocation(), false, "host-c")

        // then: a miss is evicted in favor of the pin
        assertThat(cache.size()).isEqualTo(2)
        assertThat(cache.lookup("c/1/c.jar".toLocation(), false)).isEqualTo(PinnedMirror("host-c"))
    }

    @Test
    fun `authentication buckets are isolated and exact entries can be invalidated`() {
        // given: different states for anonymous and authenticated requests
        val cache = ResolutionCache(4, NEGATIVE_CACHING, 60)
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

    private class TestClock(var now: Instant = Instant.EPOCH) : Clock() {
        override fun getZone(): ZoneId = UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }

}
