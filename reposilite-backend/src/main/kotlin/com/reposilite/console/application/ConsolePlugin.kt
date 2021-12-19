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
package com.reposilite.console.application

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.console.CommandExecutor
import com.reposilite.console.ConsoleFacade
import com.reposilite.console.HelpCommand
import com.reposilite.console.LevelCommand
import com.reposilite.console.StopCommand
import com.reposilite.console.infrastructure.CliEndpoint
import com.reposilite.console.infrastructure.ConsoleEndpoint
import com.reposilite.plugin.ReposilitePlugin
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposiliteStartedEvent
import com.reposilite.settings.SettingsFacade
import com.reposilite.status.FailureFacade
import com.reposilite.web.api.HttpServerInitializationEvent
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "console", dependencies = [ "settings", "failure", "authentication" ])
internal class ConsolePlugin : ReposilitePlugin() {

    override fun initialize(): Facade {
        val settingsFacade = facade<SettingsFacade>()
        val failureFacade = facade<FailureFacade>()
        val authenticationFacade = facade<AuthenticationFacade>()

        val commandExecutor = CommandExecutor(this, failureFacade, System.`in`)
        val consoleFacade = ConsoleFacade(this, commandExecutor)

        event { event: ReposiliteInitializeEvent ->
            consoleFacade.registerCommand(HelpCommand(consoleFacade))
            consoleFacade.registerCommand(LevelCommand(event.reposilite.journalist))
            consoleFacade.registerCommand(StopCommand(event.reposilite))

            // disable console daemon in tests due to issues with coverage and interrupt method call
            // https://github.com/jacoco/jacoco/issues/1066
            if (!event.reposilite.parameters.testEnv) {
                consoleFacade.commandExecutor.hook()
            }
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(ConsoleEndpoint(consoleFacade))
        }

        event { event: HttpServerInitializationEvent ->
            event.javalin.ws(
                "/api/console/sock",
                CliEndpoint(event.reposilite.journalist, authenticationFacade, consoleFacade, settingsFacade.sharedConfiguration.forwardedIp)
            )
        }

        event { event: ReposiliteStartedEvent ->
            event.reposilite.ioService.execute {
                consoleFacade.executeCommand("help")
                logger.info("")
                logger.info("Collecting status metrics...")
                logger.info("")
                consoleFacade.executeCommand("status")
                logger.info("")
            }
        }

        event { _: ReposiliteDisposeEvent ->
            consoleFacade.commandExecutor.stop()
        }

        return consoleFacade
    }

}