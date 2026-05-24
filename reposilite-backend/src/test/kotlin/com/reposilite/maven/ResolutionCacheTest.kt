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

import com.reposilite.maven.ResolutionCache.Origin
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.storage.api.toLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ResolutionCacheTest {

    @Test
    fun `lookup returns null on miss`() {
        // given: an empty cache
        val cache = ResolutionCache(maxEntries = 4)

        // when: a path is looked up
        val result = cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false)

        // then: nothing is returned
        assertThat(result).isNull()
    }

    @Test
    fun `remote entry pins matching host for any path under the prefix`() {
        // given: a Remote entry recorded at a GA prefix
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Remote("https://repo1.maven.org"))

        // when: paths under the prefix are looked up
        val pomLookup = cache.lookup("org/example/foo/1.0/foo.pom".toLocation(), authenticated = false)
        val jarLookup = cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false)

        // then: both inherit the pinned host
        assertThat(pomLookup).isEqualTo(Origin.Remote("https://repo1.maven.org"))
        assertThat(jarLookup).isEqualTo(Origin.Remote("https://repo1.maven.org"))
    }

    @Test
    fun `local entry marks all paths under the prefix as locally resolvable`() {
        // given: a Local entry recorded at a GA prefix
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Local)

        // when: a JAR under the prefix is looked up
        val result = cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false)

        // then: the prefix's Local origin is returned
        assertThat(result).isEqualTo(Origin.Local)
    }

    @Test
    fun `negative entry matches only at the exact prefix it was recorded for`() {
        // given: a Negative entry recorded at a metadata prefix
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/missing/foo".toLocation(), authenticated = false, origin = Origin.MissingMetadata)

        // when: looked up at exact prefix and at a deeper descendant
        val exact = cache.lookup("org/missing/foo/$METADATA_FILE".toLocation(), authenticated = false)
        val descendant = cache.lookup("org/missing/foo/1.0/foo-1.0.pom".toLocation(), authenticated = false)

        // then: Negative matches only at the exact prefix — it says "this metadata 404'd", not "every descendant is empty"
        assertThat(exact).isEqualTo(Origin.MissingMetadata)
        assertThat(descendant).isNull()
    }

    @Test
    fun `lookup walks parents until a matching prefix is found`() {
        // given: a version-level pin and a GA-level pin to different hosts
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo/1.0".toLocation(), authenticated = false, origin = Origin.Remote("https://snapshots"))
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Remote("https://central"))

        // when: paths under each prefix are looked up
        val pinned = cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false)
        val inherited = cache.lookup("org/example/foo/2.0/foo.jar".toLocation(), authenticated = false)

        // then: the closest matching prefix wins
        assertThat(pinned).isEqualTo(Origin.Remote("https://snapshots"))
        assertThat(inherited).isEqualTo(Origin.Remote("https://central"))
    }

    @Test
    fun `auth bucket is isolated`() {
        // given: distinct entries recorded for the same prefix in each auth bucket
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Local)
        cache.record("org/example/foo".toLocation(), authenticated = true, origin = Origin.Remote("https://private"))

        // when: the same path is looked up in both buckets
        val anonymous = cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false)
        val authenticated = cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = true)

        // then: each bucket returns its own entry
        assertThat(anonymous).isEqualTo(Origin.Local)
        assertThat(authenticated).isEqualTo(Origin.Remote("https://private"))
    }

    @Test
    fun `invalidate removes only the exact parent prefix entries in both auth buckets`() {
        // given: a version-level entry (both auth buckets) plus an unrelated ancestor pin
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Local)
        cache.record("org/example/foo/1.0".toLocation(), authenticated = false, origin = Origin.Remote("https://central"))
        cache.record("org/example/foo/1.0".toLocation(), authenticated = true, origin = Origin.Remote("https://private"))

        // when: invalidate is called for a file under the version prefix
        cache.invalidate("org/example/foo/1.0/foo.jar".toLocation())

        // then: the version-prefix entries are cleared in both auth buckets, but the ancestor pin survives
        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false))
            .isEqualTo(Origin.Local) // inherits from the ancestor pin at org/example/foo
        assertThat(cache.lookup("org/example/foo/$METADATA_FILE".toLocation(), authenticated = false))
            .isEqualTo(Origin.Local)
        assertThat(cache.size()).isEqualTo(1)
    }

    @Test
    fun `purge clears all entries`() {
        // given: a cache with entries across both auth buckets
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("a".toLocation(), authenticated = false, origin = Origin.Remote("x"))
        cache.record("b".toLocation(), authenticated = true, origin = Origin.MissingMetadata)
        cache.record("c".toLocation(), authenticated = false, origin = Origin.Local)

        // when: purge is called
        cache.purge()

        // then: the cache is empty
        assertThat(cache.size()).isZero()
    }

    @Test
    fun `eviction drops the lowest hit entry when over capacity`() {
        // given: a cache at capacity with a hot and a warm entry
        val cache = ResolutionCache(maxEntries = 2)
        cache.record("hot".toLocation(), authenticated = false, origin = Origin.Remote("h1"))
        cache.record("warm".toLocation(), authenticated = false, origin = Origin.Remote("h2"))
        repeat(5) { cache.lookup("hot/anything".toLocation(), authenticated = false) }
        cache.lookup("warm/anything".toLocation(), authenticated = false)

        // when: a third entry is recorded
        cache.record("cold".toLocation(), authenticated = false, origin = Origin.Remote("h3"))

        // then: the least-popular entry is evicted to stay within the cap
        assertThat(cache.size()).isEqualTo(2)
        assertThat(cache.lookup("hot/x".toLocation(), authenticated = false)).isEqualTo(Origin.Remote("h1"))
        assertThat(cache.lookup("warm/x".toLocation(), authenticated = false)).isNull()
        assertThat(cache.lookup("cold/x".toLocation(), authenticated = false)).isEqualTo(Origin.Remote("h3"))
    }

    @Test
    fun `stats returns top entries sorted by hit count`() {
        // given: three recorded entries with differing lookup counts
        val cache = ResolutionCache(maxEntries = 8)
        cache.record("a".toLocation(), authenticated = false, origin = Origin.Remote("host-a"))
        cache.record("b".toLocation(), authenticated = false, origin = Origin.Remote("host-b"))
        cache.record("c".toLocation(), authenticated = false, origin = Origin.MissingMetadata)
        repeat(3) { cache.lookup("b/anything".toLocation(), authenticated = false) }
        repeat(1) { cache.lookup("a/anything".toLocation(), authenticated = false) }

        // when: the top 2 entries are requested
        val top = cache.stats(top = 2)

        // then: they are returned in descending hit-count order
        assertThat(top.map { it.prefix.toString() }).containsExactly("b", "a")
        assertThat(top[0].origin).isEqualTo(Origin.Remote("host-b"))
        assertThat(top[0].hitCount).isEqualTo(3)
        assertThat(top[1].origin).isEqualTo(Origin.Remote("host-a"))
    }

    @Test
    fun `record at empty prefix is a no-op`() {
        // given: an empty cache
        val cache = ResolutionCache(maxEntries = 4)

        // when: a record is attempted at the empty prefix
        cache.record("".toLocation(), authenticated = false, origin = Origin.Remote("x"))

        // then: nothing is stored
        assertThat(cache.size()).isZero()
    }

    @Test
    fun `re-recording the same origin preserves accumulated hitCount`() {
        // given: a recorded entry that has accumulated lookups
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Local)
        repeat(5) { cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false) }

        // when: the same origin is recorded again
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Local)

        // then: the hit count is preserved
        assertThat(cache.stats(top = 1).single().hitCount).isEqualTo(5)
    }

    @Test
    fun `re-recording a different origin replaces the entry`() {
        // given: a recorded entry with one origin
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Remote("h1"))
        cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false)

        // when: a different origin is recorded for the same prefix
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Local)

        // then: subsequent lookups see the new origin
        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false))
            .isEqualTo(Origin.Local)
    }

}
