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

import com.reposilite.maven.ResolutionCache.Origin.Local
import com.reposilite.maven.ResolutionCache.Origin.Negative
import com.reposilite.maven.ResolutionCache.Origin.Remote
import com.reposilite.storage.api.Location
import java.util.concurrent.ConcurrentHashMap

internal class ResolutionCache(private val maxEntries: Int) {

    private data class Key(val prefix: Location, val authenticated: Boolean)

    private class Entry(val origin: Origin) {
        @Volatile var hitCount: Long = 0
    }

    sealed interface Origin {
        data object Local : Origin
        data class Remote(val host: String) : Origin
        data object Negative : Origin
    }

    data class Snapshot(val prefix: Location, val authenticated: Boolean, val origin: Origin, val hitCount: Long)

    private val entries = ConcurrentHashMap<Key, Entry>()
    private val evictionLock = Any()

    fun lookup(gav: Location, authenticated: Boolean): Origin? {
        var prefix = gav.getParent()
        while (prefix.toString().isNotEmpty()) {
            val entry = entries[Key(prefix, authenticated)]
            if (entry != null) {
                entry.hitCount++
                return entry.origin
            }
            prefix = prefix.getParent()
        }
        return null
    }

    fun record(prefix: Location, authenticated: Boolean, origin: Origin) {
        if (prefix.toString().isEmpty()) return
        val key = Key(prefix, authenticated)
        // Evict before insert so the just-recorded entry (hitCount = 0) isn't itself the victim.
        if (!entries.containsKey(key) && entries.size >= maxEntries) evictDownTo(maxEntries - 1)
        entries[key] = Entry(origin)
    }

    fun invalidate(gav: Location) {
        var prefix = gav.getParent()
        while (prefix.toString().isNotEmpty()) {
            entries.remove(Key(prefix, authenticated = true))
            entries.remove(Key(prefix, authenticated = false))
            prefix = prefix.getParent()
        }
    }

    fun purge() {
        entries.clear()
    }

    fun size(): Int =
        entries.size

    fun stats(top: Int): List<Snapshot> =
        entries.entries.asSequence()
            .map { (key, entry) -> Snapshot(key.prefix, key.authenticated, entry.origin, entry.hitCount) }
            .sortedByDescending { it.hitCount }
            .take(top)
            .toList()

    private fun evictDownTo(targetSize: Int) {
        synchronized(evictionLock) {
            while (entries.size > targetSize) {
                val victim = entries.entries.minByOrNull { it.value.hitCount } ?: return
                entries.remove(victim.key, victim.value)
            }
        }
    }

}
