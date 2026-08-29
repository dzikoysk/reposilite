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

import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.storage.api.Location
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

enum class ResolutionCacheLevel {
    PINNING,
    NEGATIVE_CACHING,
}

internal class ResolutionCache(
    private val maxEntries: Int,
    private val level: ResolutionCacheLevel,
    private val missingMaxAgeInSeconds: Long,
    private val clock: Clock = Clock.systemUTC(),
) {

    private data class Key(val location: Location, val authenticated: Boolean)

    private class Entry(val state: State, val expiresAt: Instant? = null) {
        val hitCount = LongAdder()
    }

    sealed interface State {
        data class PinnedMirror(val host: String) : State
        data object MirrorsMissing : State
    }

    data class ResolutionCacheEntry(val location: Location, val authenticated: Boolean, val state: State, val hitCount: Long)

    private val entries = ConcurrentHashMap<Key, Entry>()
    private val evictionLock = Any()

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
        if (level == ResolutionCacheLevel.NEGATIVE_CACHING && missingMaxAgeInSeconds > 0) {
            record(Key(gav, authenticated), State.MirrorsMissing, clock.instant().plusSeconds(missingMaxAgeInSeconds))
        }
    }

    fun invalidate(gav: Location) {
        entries.remove(Key(gav, authenticated = true))
        entries.remove(Key(gav, authenticated = false))
    }

    fun purge() {
        entries.clear()
    }

    fun size(): Int {
        removeExpired()
        return entries.size
    }

    fun stats(top: Int): List<ResolutionCacheEntry> {
        removeExpired()
        return entries.entries.asSequence()
            .map { (key, entry) -> ResolutionCacheEntry(key.location, key.authenticated, entry.state, entry.hitCount.sum()) }
            .sortedByDescending { it.hitCount }
            .take(top.coerceAtLeast(0))
            .toList()
    }

    private inline fun <reified T : State> find(key: Key): T? {
        val entry = entries[key] ?: return null
        if (entry.expiresAt?.isAfter(clock.instant()) == false) {
            entries.remove(key, entry)
            return null
        }
        if (entry.state !is T) {
            return null
        }
        entry.hitCount.increment()
        return entry.state
    }

    private fun record(key: Key, state: State, expiresAt: Instant? = null) {
        if (key.location == Location.empty()) {
            return
        }
        synchronized(evictionLock) {
            if (entries[key]?.state == state) {
                return
            }
            if (!entries.containsKey(key) && entries.size >= maxEntries && !evictFor(state)) {
                return
            }
            entries[key] = Entry(state, expiresAt)
        }
    }

    private fun evictFor(state: State): Boolean {
        val now = clock.instant()
        val victim = entries.entries.asSequence()
            .filter { it.value.expiresAt?.isAfter(now) == false }
            .minByOrNull { it.value.hitCount.sum() }
            ?: entries.entries.asSequence()
                .filter { it.value.state == State.MirrorsMissing }
                .minByOrNull { it.value.hitCount.sum() }
            ?: entries.entries
                .takeIf { state is State.PinnedMirror }
                ?.asSequence()
                ?.minByOrNull { it.value.hitCount.sum() }
            ?: return false

        return entries.remove(victim.key, victim.value)
    }

    private fun removeExpired() {
        val now = clock.instant()
        entries.entries.removeIf { it.value.expiresAt?.isAfter(now) == false }
    }

}
