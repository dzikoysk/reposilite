/*
 * Copyright (c) 2021 dzikoysk
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
package com.reposilite.console

import com.reposilite.Reposilite
import com.reposilite.VERSION
import com.reposilite.console.Status.SUCCEEDED
import com.reposilite.shared.TimeUtils
import com.reposilite.shared.TimeUtils.getPrettyUptimeInMinutes
import panda.utilities.IOUtils
import panda.utilities.console.Effect.GREEN
import panda.utilities.console.Effect.GREEN_BOLD
import panda.utilities.console.Effect.RED_UNDERLINED
import panda.utilities.console.Effect.RESET
import picocli.CommandLine.Command

@Command(name = "status", description = ["Display summary status of app health"])
internal class StatusCommand(
    private val reposilite: Reposilite,
    private val remoteVersionUrl: String,
) : ReposiliteCommand {

    override fun execute(output: MutableList<String>): Status {
        val latestVersion =
            if (reposilite.parameters.testEnv) VERSION
            else getVersion()

        output.add("Reposilite $VERSION Status")
        output.add("  Active: $GREEN_BOLD${reposilite.webServer.isAlive()}$RESET")
        output.add("  Uptime: ${getPrettyUptimeInMinutes(reposilite.startTime)}")
        output.add("  Memory usage of process: ${memoryUsage()}")
        output.add("  Active threads in group: ${threadGroupUsage()}")
        output.add("  Recorded failures: ${reposilite.failureFacade.getFailures().size}")
        output.add("  Latest version of Reposilite: $latestVersion")

        return SUCCEEDED
    }

    private fun getVersion(): String =
        IOUtils.fetchContent(remoteVersionUrl)
            .orElseGet { "$remoteVersionUrl is unavailable: ${it.message}" }
            .let { (if (VERSION == it) GREEN else RED_UNDERLINED).toString() + it + RESET }

    private fun memoryUsage(): String =
        TimeUtils.format((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0) + "M"

    private fun threadGroupUsage(): String =
        Thread.activeCount().toString()

}