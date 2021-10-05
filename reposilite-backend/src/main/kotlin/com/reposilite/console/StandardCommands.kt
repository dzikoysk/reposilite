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
import com.reposilite.ReposiliteJournalist
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.api.ReposiliteCommand
import com.reposilite.journalist.Channel
import com.reposilite.shared.createCommandHelp
import panda.std.Option.ofOptional
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.util.concurrent.TimeUnit.SECONDS

@Command(name = "help", aliases = ["?"], helpCommand = true, description = ["List of available commands"])
internal class HelpCommand(private val consoleFacade: ConsoleFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "[<command>]", description = ["Display usage of the given command"], defaultValue = "")
    private lateinit var requestedCommand: String

    override fun execute(context: CommandContext) {
        createCommandHelp(consoleFacade.getCommands(), requestedCommand)
            .peek { context.appendAll(it) }
            .onError {
                context.append(it)
                context.status = FAILED
            }
    }

}

@Command(name = "stop", aliases = ["shutdown"], description = ["Shutdown server"])
internal class StopCommand(private val reposilite: Reposilite) : ReposiliteCommand {

    override fun execute(context: CommandContext) {
        reposilite.logger.warn("The shutdown request has been sent")
        reposilite.scheduler.schedule({ reposilite.shutdown() }, 1, SECONDS)
    }

}

@Command(name = "level", description = ["Change current level of visible logging"])
internal class LevelCommand(private val journalist: ReposiliteJournalist) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<level>", description = ["The new threshold"], defaultValue = "info")
    private lateinit var level: String

    override fun execute(context: CommandContext) {
        ofOptional(Channel.of(level))
            .onEmpty {
                context.status = FAILED
                context.append("The new logging level has been set to $level")
            }
            .peek {
                journalist.setVisibleThreshold(it)
                context.append("The new logging level has been set to $level")
            }
    }

}