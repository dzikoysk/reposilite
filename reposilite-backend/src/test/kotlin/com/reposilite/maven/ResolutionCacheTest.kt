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
import com.reposilite.storage.api.toLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ResolutionCacheTest {

    @Test
    fun `lookup returns null on miss`() {
        val cache = ResolutionCache(maxEntries = 4)
        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false)).isNull()
    }

    @Test
    fun `remote entry pins matching host for any path under the prefix`() {
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Remote("https://repo1.maven.org"))

        val pomLookup = cache.lookup("org/example/foo/1.0/foo.pom".toLocation(), authenticated = false)
        val jarLookup = cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false)

        assertThat(pomLookup).isEqualTo(Origin.Remote("https://repo1.maven.org"))
        assertThat(jarLookup).isEqualTo(Origin.Remote("https://repo1.maven.org"))
    }

    @Test
    fun `local entry marks all paths under the prefix as locally resolvable`() {
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Local)

        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false))
            .isEqualTo(Origin.Local)
    }

    @Test
    fun `negative entry short-circuits any path under the prefix`() {
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/missing/foo".toLocation(), authenticated = false, origin = Origin.Negative)

        assertThat(cache.lookup("org/missing/foo/1.0/foo.jar".toLocation(), authenticated = false))
            .isEqualTo(Origin.Negative)
    }

    @Test
    fun `lookup walks parents until a matching prefix is found`() {
        val cache = ResolutionCache(maxEntries = 4)
        // Version-level metadata pin applies only to that version.
        cache.record("org/example/foo/1.0".toLocation(), authenticated = false, origin = Origin.Remote("https://snapshots"))
        // GA-level entry applies to every other version under the GA.
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Remote("https://central"))

        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false))
            .isEqualTo(Origin.Remote("https://snapshots"))
        assertThat(cache.lookup("org/example/foo/2.0/foo.jar".toLocation(), authenticated = false))
            .isEqualTo(Origin.Remote("https://central"))
    }

    @Test
    fun `auth bucket is isolated`() {
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Negative)
        cache.record("org/example/foo".toLocation(), authenticated = true, origin = Origin.Remote("https://private"))

        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false))
            .isEqualTo(Origin.Negative)
        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = true))
            .isEqualTo(Origin.Remote("https://private"))
    }

    @Test
    fun `invalidate removes every cached prefix on the path of the deployed gav`() {
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("org/example/foo".toLocation(), authenticated = false, origin = Origin.Negative)
        cache.record("org/example/foo/1.0".toLocation(), authenticated = true, origin = Origin.Remote("https://central"))

        cache.invalidate("org/example/foo/1.0/foo.jar".toLocation())

        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = false)).isNull()
        assertThat(cache.lookup("org/example/foo/1.0/foo.jar".toLocation(), authenticated = true)).isNull()
        assertThat(cache.size()).isZero()
    }

    @Test
    fun `purge clears all entries`() {
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("a".toLocation(), authenticated = false, origin = Origin.Remote("x"))
        cache.record("b".toLocation(), authenticated = true, origin = Origin.Negative)
        cache.record("c".toLocation(), authenticated = false, origin = Origin.Local)

        cache.purge()

        assertThat(cache.size()).isZero()
    }

    @Test
    fun `eviction drops the lowest hit entry when over capacity`() {
        val cache = ResolutionCache(maxEntries = 2)
        cache.record("hot".toLocation(), authenticated = false, origin = Origin.Remote("h1"))
        cache.record("warm".toLocation(), authenticated = false, origin = Origin.Remote("h2"))

        // Warm up "hot" so it has the highest hit count.
        repeat(5) { cache.lookup("hot/anything".toLocation(), authenticated = false) }
        cache.lookup("warm/anything".toLocation(), authenticated = false)

        // Insert a third entry; cache must evict to stay within cap, dropping the least-popular one.
        cache.record("cold".toLocation(), authenticated = false, origin = Origin.Remote("h3"))

        assertThat(cache.size()).isEqualTo(2)
        assertThat(cache.lookup("hot/x".toLocation(), authenticated = false)).isEqualTo(Origin.Remote("h1"))
        // "warm" had only 1 hit and gets evicted in favour of "hot" (6 hits at eviction time) and the new "cold".
        assertThat(cache.lookup("warm/x".toLocation(), authenticated = false)).isNull()
        assertThat(cache.lookup("cold/x".toLocation(), authenticated = false)).isEqualTo(Origin.Remote("h3"))
    }

    @Test
    fun `stats returns top entries sorted by hit count`() {
        val cache = ResolutionCache(maxEntries = 8)
        cache.record("a".toLocation(), authenticated = false, origin = Origin.Remote("host-a"))
        cache.record("b".toLocation(), authenticated = false, origin = Origin.Remote("host-b"))
        cache.record("c".toLocation(), authenticated = false, origin = Origin.Negative)

        repeat(3) { cache.lookup("b/anything".toLocation(), authenticated = false) }
        repeat(1) { cache.lookup("a/anything".toLocation(), authenticated = false) }

        val top = cache.stats(top = 2)

        assertThat(top.map { it.prefix.toString() }).containsExactly("b", "a")
        assertThat(top[0].origin).isEqualTo(Origin.Remote("host-b"))
        assertThat(top[0].hitCount).isEqualTo(3)
        assertThat(top[1].origin).isEqualTo(Origin.Remote("host-a"))
    }

    @Test
    fun `record at empty prefix is a no-op`() {
        val cache = ResolutionCache(maxEntries = 4)
        cache.record("".toLocation(), authenticated = false, origin = Origin.Remote("x"))
        assertThat(cache.size()).isZero()
    }

}
