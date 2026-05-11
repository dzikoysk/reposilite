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

import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.api.ReposiliteCommand
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

private const val DEFAULT_TOP = 20

@Command(name = "cache-stats", description = ["Print resolution cache size and top entries by hit count"])
internal class CacheStatsCommand(private val mavenFacade: MavenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "[<repository>]", defaultValue = "", description = ["Limit output to a single repository. Defaults to all."])
    private lateinit var repository: String

    @Option(names = ["--top"], description = ["Number of top entries to display per repository (default: $DEFAULT_TOP)."], defaultValue = "$DEFAULT_TOP")
    private var top: Int = DEFAULT_TOP

    override fun execute(context: CommandContext) {
        val repositories = mavenFacade.getRepositories()
            .filter { repository.isEmpty() || it.name == repository }

        if (repositories.isEmpty()) {
            context.append("No repository named '$repository' found")
            context.status = FAILED
            return
        }

        repositories.forEach { repo ->
            val cache = repo.resolutionCache
            if (cache == null) {
                context.append("${repo.name}: resolution cache disabled")
                return@forEach
            }
            context.append("${repo.name}: ${cache.size()} cached entries")
            val snapshots = cache.stats(top)
            if (snapshots.isEmpty()) {
                context.append("  ~ no entries ~")
                return@forEach
            }
            snapshots.forEach { snap ->
                val target = when (val origin = snap.origin) {
                    ResolutionCache.Origin.Local -> "(local)"
                    is ResolutionCache.Origin.Remote -> origin.host
                    ResolutionCache.Origin.Negative -> "(not found)"
                }
                val auth = if (snap.authenticated) "auth" else "anon"
                context.append("  [${auth}] /${snap.prefix} -> $target  (hits: ${snap.hitCount})")
            }
        }
    }
}

@Command(name = "cache-purge", description = ["Purge a named Reposilite cache. Today only 'resolution' is supported."])
internal class CachePurgeCommand(private val mavenFacade: MavenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<target>", description = ["Cache to purge. Supported: 'resolution'."])
    private lateinit var target: String

    @Parameters(index = "1", paramLabel = "[<repository>]", defaultValue = "", description = ["Limit purge to a single repository. Defaults to all."])
    private lateinit var repository: String

    override fun execute(context: CommandContext) {
        if (target != "resolution") {
            context.append("Unknown cache '$target'. Supported: resolution")
            context.status = FAILED
            return
        }

        val repositories = mavenFacade.getRepositories()
            .filter { repository.isEmpty() || it.name == repository }

        if (repositories.isEmpty()) {
            context.append("No repository named '$repository' found")
            context.status = FAILED
            return
        }

        var totalCleared = 0
        var affected = 0
        repositories.forEach { repo ->
            val cache = repo.resolutionCache ?: return@forEach
            val before = cache.size()
            cache.purge()
            totalCleared += before
            affected++
        }
        context.append("Cleared $totalCleared resolution cache entries across $affected repositor${if (affected == 1) "y" else "ies"}.")
    }
}
