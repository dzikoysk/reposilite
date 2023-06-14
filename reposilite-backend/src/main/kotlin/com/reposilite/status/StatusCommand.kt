/*
 * Copyright (c) 2023 dzikoysk
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
import com.reposilite.shared.extensions.TimeUtils
import panda.utilities.console.Effect.GREEN
import panda.utilities.console.Effect.GREEN_BOLD
import panda.utilities.console.Effect.RED_UNDERLINED
import panda.utilities.console.Effect.RESET
import picocli.CommandLine.Command

@Command(name = "status", description = ["Display summary status of app health"])
internal class StatusCommand(private val statusFacade: StatusFacade) : ReposiliteCommand {

    override fun execute(context: CommandContext) {
        statusFacade.fetchInstanceStatus().apply {
            context.append("Reposilite $VERSION Status")
            context.append("  Active: $GREEN_BOLD${statusFacade.isAlive()}$RESET")
            context.append("  Uptime: ${TimeUtils.getPrettyUptime(statusFacade.getUptime())}")
            context.append("  Memory usage of process: ${TimeUtils.format(usedMemory)}M")
            context.append("  Active threads in group: $usedThreads")
            context.append("  Recorded failures: $failuresCount")
        }

        statusFacade.getLatestVersion()
            .fold(
                { "${if (VERSION == it) GREEN else RED_UNDERLINED}$it$RESET" },
                { "$RED_UNDERLINED$it${RESET}" }
            )
            .let { coloredStatus -> context.append("  Latest version of Reposilite: $coloredStatus") }
    }

}
