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

import com.reposilite.Reposilite
import com.reposilite.console.CommandExecutor
import com.reposilite.console.ConsoleFacade
import com.reposilite.console.HelpCommand
import com.reposilite.console.LevelCommand
import com.reposilite.console.StopCommand
import com.reposilite.console.infrastructure.CliEndpoint
import com.reposilite.console.infrastructure.ConsoleEndpoint
import com.reposilite.journalist.Journalist
import com.reposilite.status.FailureFacade
import com.reposilite.web.ReposiliteRoutes
import com.reposilite.web.WebConfiguration
import io.javalin.Javalin

internal object ConsoleWebConfiguration : WebConfiguration {

    fun createFacade(journalist: Journalist, failureFacade: FailureFacade): ConsoleFacade {
        val commandExecutor = CommandExecutor(journalist, failureFacade, System.`in`)
        return ConsoleFacade(journalist, commandExecutor)
    }

    override fun initialize(reposilite: Reposilite) {
        val consoleFacade = reposilite.consoleFacade
        consoleFacade.registerCommand(HelpCommand(consoleFacade))
        consoleFacade.registerCommand(LevelCommand(reposilite.journalist))
        consoleFacade.registerCommand(StopCommand(reposilite))

        // disable console daemon in tests due to issues with coverage and interrupt method call
        // https://github.com/jacoco/jacoco/issues/1066
        if (!reposilite.parameters.testEnv) {
            consoleFacade.commandExecutor.hook()
        }
    }

    override fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> = setOf(
        ConsoleEndpoint(reposilite.consoleFacade)
    )

    override fun javalin(reposilite: Reposilite, javalin: Javalin) {
        javalin.ws("/api/console/sock", CliEndpoint(reposilite.journalist, reposilite.authenticationFacade, reposilite.consoleFacade, reposilite.configuration.forwardedIp))
    }

    override fun dispose(reposilite: Reposilite) {
        reposilite.consoleFacade.commandExecutor.stop()
    }

}