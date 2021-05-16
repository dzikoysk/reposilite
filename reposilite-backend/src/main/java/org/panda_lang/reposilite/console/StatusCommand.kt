/*
 * Copyright (c) 2020 Dzikoysk
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
package org.panda_lang.reposilite.console

import org.panda_lang.reposilite.Reposilite
import org.panda_lang.reposilite.ReposiliteConstants
import org.panda_lang.reposilite.utils.TimeUtils.format
import org.panda_lang.utilities.commons.IOUtils
import org.panda_lang.utilities.commons.console.Effect.GREEN
import org.panda_lang.utilities.commons.console.Effect.GREEN_BOLD
import org.panda_lang.utilities.commons.console.Effect.RED_UNDERLINED
import org.panda_lang.utilities.commons.console.Effect.RESET
import picocli.CommandLine.Command
import java.io.IOException

@Command(name = "status", description = ["Display summary status of app health"])
internal class StatusCommand(private val reposilite: Reposilite) : ReposiliteCommand {

    override fun execute(output: MutableList<String>): Boolean {
        val latestVersion =
            if (reposilite.testEnv) ReposiliteConstants.VERSION
            else getVersion()

        output.add("Reposilite ${ReposiliteConstants.VERSION} Status")
        output.add("  Active: $GREEN_BOLD${reposilite.httpServer.isAlive}$RESET")
        output.add("  Uptime: ${format(reposilite.getUptime() / 1000.0 / 60.0)}min")
        output.add("  Memory usage of process: ${memoryUsage()}")
        output.add("  Exceptions: ${reposilite.failureFacade.getFailures().size}")
        output.add("  Latest version of reposilite: $latestVersion")

        return true
    }

    private fun getVersion(): String =
        IOUtils
            .fetchContent(ReposiliteConstants.REMOTE_VERSION)
            .orElseGet { ioException: IOException -> ReposiliteConstants.REMOTE_VERSION + " is unavailable: " + ioException.message }
            .let { (if (ReposiliteConstants.VERSION == it) GREEN else RED_UNDERLINED).toString() + it + RESET }

    private fun memoryUsage(): String =
        format((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0) + "M"

}