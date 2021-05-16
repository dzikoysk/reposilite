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
package org.panda_lang.reposilite.console.application

import net.dzikoysk.dynamiclogger.Journalist
import org.panda_lang.reposilite.Reposilite
import org.panda_lang.reposilite.console.Console
import org.panda_lang.reposilite.console.ConsoleFacade
import org.panda_lang.reposilite.console.HelpCommand
import org.panda_lang.reposilite.console.StatusCommand
import org.panda_lang.reposilite.console.StopCommand
import org.panda_lang.reposilite.console.VersionCommand
import org.panda_lang.reposilite.failure.FailureFacade

class ConsoleWebConfiguration {

    fun createFacade(journalist: Journalist, failureFacade: FailureFacade): ConsoleFacade {
        val console = Console(journalist, failureFacade, System.`in`)
        return ConsoleFacade(journalist, console)
    }

    fun initialize(consoleFacade: ConsoleFacade, reposilite: Reposilite) {
        consoleFacade.registerCommand(HelpCommand(consoleFacade))
        consoleFacade.registerCommand(StatusCommand(reposilite))
        consoleFacade.registerCommand(StopCommand(reposilite))
        consoleFacade.registerCommand(VersionCommand())
    }

}