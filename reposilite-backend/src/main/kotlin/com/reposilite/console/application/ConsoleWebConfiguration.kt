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
import com.reposilite.console.Console
import com.reposilite.console.ConsoleFacade
import com.reposilite.console.HelpCommand
import com.reposilite.console.StatusCommand
import com.reposilite.console.StopCommand
import com.reposilite.console.VersionCommand
import com.reposilite.console.infrastructure.CliEndpoint
import com.reposilite.console.infrastructure.RemoteExecutionEndpoint
import com.reposilite.failure.FailureFacade
import com.reposilite.web.ReposiliteRoutes
import io.javalin.Javalin
import net.dzikoysk.dynamiclogger.Journalist

private const val REMOTE_VERSION = "https://repo.panda-lang.org/org/panda-lang/reposilite/latest"

internal object ConsoleWebConfiguration {

    fun createFacade(journalist: Journalist, failureFacade: FailureFacade): ConsoleFacade {
        val console = Console(journalist, failureFacade, System.`in`)
        return ConsoleFacade(journalist, console)
    }

    fun initialize(consoleFacade: ConsoleFacade, reposilite: Reposilite) {
        consoleFacade.registerCommand(HelpCommand(consoleFacade))
        consoleFacade.registerCommand(StatusCommand(reposilite, REMOTE_VERSION))
        consoleFacade.registerCommand(StopCommand(reposilite))
        consoleFacade.registerCommand(VersionCommand())

        // disable console daemon in tests due to issues with coverage and interrupt method call
        // https://github.com/jacoco/jacoco/issues/1066
        if (!reposilite.parameters.testEnv) {
            consoleFacade.console.hook()
        }
    }

    fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> =
        setOf(
            RemoteExecutionEndpoint(reposilite.consoleFacade)
        )

    fun javalin(javalin: Javalin, reposilite: Reposilite) {
        javalin.ws("/api/cli", CliEndpoint(reposilite.contextFactory, reposilite.authenticationFacade, reposilite.consoleFacade, reposilite.cachedLogger))
    }

    fun dispose(consoleFacade: ConsoleFacade) {
        consoleFacade.console.stop()
    }

}