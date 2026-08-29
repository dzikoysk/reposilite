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

package com.reposilite.maven.infrastructure

import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.api.ReposiliteCommand
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.Repository
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "cache", description = ["Purge a Reposilite cache. Usage: cache <target> <action> [<repository>]. Targets: resolution. Actions: purge."])
internal class CacheCommand(private val mavenFacade: MavenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<target>", description = ["Cache to operate on. Supported: 'resolution'."])
    private lateinit var target: String

    @Parameters(index = "1", paramLabel = "<action>", description = ["Action to perform. Supported: 'purge'."])
    private lateinit var action: String

    @Parameters(index = "2", paramLabel = "[<repository>]", defaultValue = "", description = ["Limit to a single repository. Defaults to all."])
    private lateinit var repository: String

    override fun execute(context: CommandContext) {
        when (target) {
            "resolution" -> dispatch(context)
            else -> context.fail("Unknown cache '$target'. Supported: resolution")
        }
    }

    private fun dispatch(context: CommandContext) {
        val repositories = mavenFacade.getRepositories()
            .filter { repository.isEmpty() || it.name == repository }

        if (repositories.isEmpty()) {
            context.fail("No repository named '$repository' found")
            return
        }

        when (action) {
            "purge" -> purge(repositories, context)
            else -> context.fail("Unknown action '$action'. Supported: purge")
        }
    }

    private fun purge(repositories: List<Repository>, context: CommandContext) {
        var totalCleared = 0
        var affected = 0
        repositories.forEach { repo ->
            val cache = repo.resolutionCache ?: return@forEach
            totalCleared += cache.purge()
            affected++
        }
        context.append("Cleared $totalCleared resolution cache entries across $affected repositor${if (affected == 1) "y" else "ies"}.")
    }

    private fun CommandContext.fail(message: String) {
        append(message)
        status = FAILED
    }
}
