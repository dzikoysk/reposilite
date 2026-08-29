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

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.storage.api.Location
import java.util.concurrent.atomic.LongAdder

enum class ResolutionCacheLevel {
    PINNING,
    NEGATIVE_CACHING,
}

internal class ResolutionCache(
    maxEntries: Int,
    private val level: ResolutionCacheLevel,
) {

    private data class Key(val location: Location, val authenticated: Boolean)

    private class Entry(val state: State) {
        val hitCount = LongAdder()
    }

    sealed interface State {
        data class PinnedMirror(val host: String) : State
        data object MirrorsMissing : State
    }

    data class ResolutionCacheEntry(val location: Location, val authenticated: Boolean, val state: State, val hitCount: Long)

    private val entries: Cache<Key, Entry> =
        Caffeine.newBuilder()
            .maximumSize(maxEntries.toLong())
            .build()

    fun lookup(gav: Location, authenticated: Boolean): State? {
        find<State.MirrorsMissing>(Key(gav, authenticated))?.let { return it }

        val direct = gav.getParent()
        find<State.PinnedMirror>(Key(direct, authenticated))?.let { return it }

        return when (gav.getSimpleName()) {
            METADATA_FILE -> null
            else -> find<State.PinnedMirror>(Key(direct.getParent(), authenticated))
        }
    }

    fun recordPinnedMirror(gav: Location, authenticated: Boolean, host: String) {
        record(Key(gav.getParent(), authenticated), State.PinnedMirror(host))
    }

    fun recordMirrorsMissing(gav: Location, authenticated: Boolean) {
        if (level == ResolutionCacheLevel.NEGATIVE_CACHING) {
            record(Key(gav, authenticated), State.MirrorsMissing)
        }
    }

    fun invalidate(gav: Location) {
        entries.invalidate(Key(gav, authenticated = true))
        entries.invalidate(Key(gav, authenticated = false))
    }

    fun purge() {
        entries.invalidateAll()
    }

    fun size(): Int {
        entries.cleanUp()
        return entries.estimatedSize().toInt()
    }

    fun stats(top: Int): List<ResolutionCacheEntry> {
        entries.cleanUp()
        return entries.asMap().entries.asSequence()
            .map { (key, entry) -> ResolutionCacheEntry(key.location, key.authenticated, entry.state, entry.hitCount.sum()) }
            .sortedByDescending { it.hitCount }
            .take(top.coerceAtLeast(0))
            .toList()
    }

    private inline fun <reified T : State> find(key: Key): T? {
        val entry = entries.getIfPresent(key) ?: return null
        if (entry.state !is T) {
            return null
        }
        entry.hitCount.increment()
        return entry.state
    }

    private fun record(key: Key, state: State) {
        if (key.location == Location.empty()) {
            return
        }
        entries.asMap().compute(key) { _, entry ->
            entry?.takeIf { it.state == state } ?: Entry(state)
        }
    }

}
