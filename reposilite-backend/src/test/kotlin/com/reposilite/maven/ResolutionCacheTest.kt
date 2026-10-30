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

import com.github.benmanes.caffeine.cache.Ticker
import com.reposilite.maven.ResolutionCache.State.MirrorsMissing
import com.reposilite.maven.ResolutionCache.State.PinnedMirror
import com.reposilite.maven.ResolutionCacheLevel.NEGATIVE_CACHING
import com.reposilite.maven.ResolutionCacheLevel.PINNING
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.storage.api.toLocation
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
        assertThat(cache.purge()).isEqualTo(2)
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

    @Test
    fun `misses expire after the configured ttl`() {
        // given: a negative cache with a 5 minute miss ttl and a recorded miss
        val time = FakeTicker()
        val cache = ResolutionCache(4, NEGATIVE_CACHING, missTtl = Duration.ofMinutes(5), ticker = time)
        val metadata = "org/example/foo/$METADATA_FILE".toLocation()
        cache.recordMirrorsMissing(metadata, false)

        // when: time passes up to, and then beyond, the ttl
        time.advance(Duration.ofMinutes(5).minusSeconds(1))
        val beforeTtl = cache.lookup(metadata, false)
        time.advance(Duration.ofSeconds(1))
        val afterTtl = cache.lookup(metadata, false)

        // then: the miss is served until the ttl elapses and then forgotten
        assertThat(beforeTtl).isEqualTo(MirrorsMissing)
        assertThat(afterTtl).isNull()
    }

    @Test
    fun `reading a miss does not extend its ttl`() {
        // given: a recorded miss with a 1 minute ttl
        val time = FakeTicker()
        val cache = ResolutionCache(4, NEGATIVE_CACHING, missTtl = Duration.ofMinutes(1), ticker = time)
        val metadata = "org/example/foo/$METADATA_FILE".toLocation()
        cache.recordMirrorsMissing(metadata, false)

        // when: the miss is read repeatedly while time passes
        repeat(5) {
            time.advance(Duration.ofSeconds(15))
            cache.lookup(metadata, false)
        }

        // then: the ttl counts from when the miss was recorded, not from the last read
        assertThat(cache.lookup(metadata, false)).isNull()
    }

    @Test
    fun `pins never expire by time even with a miss ttl`() {
        // given: a cache with a short miss ttl holding a pin and a miss
        val time = FakeTicker()
        val cache = ResolutionCache(4, NEGATIVE_CACHING, missTtl = Duration.ofSeconds(30), ticker = time)
        cache.recordPinnedMirror("org/example/foo/$METADATA_FILE".toLocation(), false, "central")
        cache.recordMirrorsMissing("org/example/bar/$METADATA_FILE".toLocation(), false)

        // when: far more time than the ttl passes
        time.advance(Duration.ofDays(30))

        // then: the miss is gone but the routing pin still applies
        assertThat(cache.lookup("org/example/bar/$METADATA_FILE".toLocation(), false)).isNull()
        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), false)).isEqualTo(PinnedMirror("central"))
    }

    @Test
    fun `zero miss ttl keeps misses until invalidation`() {
        // given: the default configuration, with no miss ttl
        val time = FakeTicker()
        val cache = ResolutionCache(4, NEGATIVE_CACHING, ticker = time)
        val metadata = "org/example/foo/$METADATA_FILE".toLocation()
        cache.recordMirrorsMissing(metadata, false)

        // when: a long time passes
        time.advance(Duration.ofDays(365))

        // then: the miss is retained exactly as before this setting existed
        assertThat(cache.lookup(metadata, false)).isEqualTo(MirrorsMissing)
    }

    @Test
    fun `negative miss ttl is rejected`() {
        assertThatThrownBy { ResolutionCache(4, NEGATIVE_CACHING, missTtl = Duration.ofSeconds(-1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private class FakeTicker : Ticker {

        private val nanos = AtomicLong()

        fun advance(duration: Duration) {
            nanos.addAndGet(duration.toNanos())
        }

        override fun read(): Long =
            nanos.get()

    }

}
