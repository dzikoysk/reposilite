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

enum class ResolutionCacheLevel {
    PINNING,
    NEGATIVE_CACHING,
}

internal class ResolutionCache(
    maxEntries: Int,
    private val level: ResolutionCacheLevel,
) {

    private data class Key(val location: Location, val authenticated: Boolean)

    sealed interface State {
        data class PinnedMirror(val host: String) : State
        data object MirrorsMissing : State
    }

    private val entries: Cache<Key, State> =
        Caffeine.newBuilder()
            .maximumSize(maxEntries.toLong())
            .build()

    fun lookup(gav: Location, authenticated: Boolean): State? {
        val missing = entries.getIfPresent(Key(gav, authenticated))
        if (missing is State.MirrorsMissing) {
            return missing
        }

        val parent = gav.getParent()
        val pinned = entries.getIfPresent(Key(parent, authenticated))
        if (pinned is State.PinnedMirror) {
            return pinned
        }

        return when (gav.getSimpleName()) {
            METADATA_FILE -> null
            else -> entries.getIfPresent(Key(parent.getParent(), authenticated)) as? State.PinnedMirror
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

    fun purge(): Int {
        entries.cleanUp()
        val size = entries.estimatedSize().toInt()
        entries.invalidateAll()
        return size
    }

    private fun record(key: Key, state: State) {
        if (key.location == Location.empty()) {
            return
        }
        entries.put(key, state)
    }

}
