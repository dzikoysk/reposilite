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

import com.reposilite.storage.api.Location
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

internal class ResolutionCache(private val maxEntries: Int) {

    private data class Key(val prefix: Location, val authenticated: Boolean)

    private class Entry(val origin: Origin) {
        val hitCount = LongAdder()
    }

    sealed interface Origin {
        data object Local : Origin
        data class Remote(val host: String) : Origin
        // Recorded when any resolution-metadata file (maven-metadata.xml, *.pom, etc.) at this exact prefix
        // is missing both locally and from every probed mirror. Does not inherit to descendants.
        data object MissingMetadata : Origin
    }

    data class ResolutionCacheEntry(val prefix: Location, val authenticated: Boolean, val origin: Origin, val hitCount: Long)

    private val entries = ConcurrentHashMap<Key, Entry>()
    private val evictionLock = Any()

    fun lookup(gav: Location, authenticated: Boolean): Origin? {
        val exactPrefix = gav.getParent()
        var prefix = exactPrefix
        while (prefix != Location.empty()) {
            val entry = entries[Key(prefix, authenticated)]
            if (entry != null) {
                // Negative is authoritative only at the exact prefix — it says "this metadata file returned 404",
                // not "every descendant is empty." Local/Remote pins still inherit down (routing decisions).
                if (entry.origin is Origin.MissingMetadata && prefix != exactPrefix) {
                    prefix = prefix.getParent()
                    continue
                }
                entry.hitCount.increment()
                return entry.origin
            }
            prefix = prefix.getParent()
        }
        return null
    }

    fun record(prefix: Location, authenticated: Boolean, origin: Origin) {
        if (prefix == Location.empty()) {
            return
        }
        val key = Key(prefix, authenticated)
        // preserve accumulated hitCount on same-origin refresh
        if (entries[key]?.origin == origin) {
            return
        }
        // Size check + insert is intentionally not synchronized — maxEntries is a soft cap that may
        // transiently overshoot under concurrent writes; the next record self-heals via eviction.
        if (!entries.containsKey(key) && entries.size >= maxEntries) {
            evictDownTo(maxEntries - 1)
        }
        entries[key] = Entry(origin)
    }

    fun invalidate(gav: Location) {
        // Only the exact parent prefix — ancestor pins (e.g. group-level routing) remain valid after a child write.
        val prefix = gav.getParent()
        if (prefix == Location.empty()) {
            return
        }
        entries.remove(Key(prefix, authenticated = true))
        entries.remove(Key(prefix, authenticated = false))
    }

    fun purge() {
        entries.clear()
    }

    fun size(): Int =
        entries.size

    fun stats(top: Int): List<ResolutionCacheEntry> =
        entries.entries.asSequence()
            .map { (key, entry) -> ResolutionCacheEntry(key.prefix, key.authenticated, entry.origin, entry.hitCount.sum()) }
            .sortedByDescending { it.hitCount }
            .take(top)
            .toList()

    private fun evictDownTo(targetSize: Int) {
        synchronized(evictionLock) {
            while (entries.size > targetSize) {
                val victim = entries.entries.minByOrNull { it.value.hitCount.sum() } ?: return
                entries.remove(victim.key, victim.value)
            }
        }
    }

}
