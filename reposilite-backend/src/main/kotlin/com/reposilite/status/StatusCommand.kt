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

package com.reposilite.status

import com.reposilite.VERSION
import com.reposilite.console.CommandContext
import com.reposilite.console.api.ReposiliteCommand
import com.sun.management.UnixOperatingSystemMXBean
import panda.utilities.console.Effect.GREEN_BOLD
import panda.utilities.console.Effect.RESET
import picocli.CommandLine.Command
import java.lang.management.ManagementFactory

@Command(name = "status", description = ["Display summary status of app health"])
internal class StatusCommand(
    private val statusFacade: StatusFacade,
    private val failureFacade: FailureFacade
) : ReposiliteCommand {

    override fun execute(context: CommandContext) {
        context.append("Reposilite $VERSION Status")
        context.append("  Active: $GREEN_BOLD${statusFacade.isAlive()}$RESET")
        context.append("  Uptime: ${statusFacade.uptime()}")
        context.append("  Memory usage of process: ${statusFacade.memoryUsage()}")
        context.append("  Active threads in group: ${statusFacade.threadGroupUsage()}")
        context.append("  Recorded failures: ${failureFacade.getFailures().size}")
        context.append("  Latest version of Reposilite: ${statusFacade.getVersion()}")

        val os = ManagementFactory.getOperatingSystemMXBean()

        if (os is UnixOperatingSystemMXBean) {
            context.append("  Number of open fd: ${os.openFileDescriptorCount}/${os.maxFileDescriptorCount}")
        }
    }

}