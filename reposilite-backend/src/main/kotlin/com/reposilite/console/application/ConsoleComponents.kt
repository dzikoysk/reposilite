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

package com.reposilite.console.application

import com.reposilite.console.CommandExecutor
import com.reposilite.console.ConsoleFacade
import com.reposilite.journalist.Journalist
import com.reposilite.status.FailureFacade
import java.io.InputStream

internal class ConsoleComponents(
    private val journalist: Journalist,
    private val failureFacade: FailureFacade
) {

    private fun consoleInput(): InputStream =
        System.`in`

    private fun commandExecutor(): CommandExecutor =
        CommandExecutor(
            journalist = journalist,
            failureFacade = failureFacade,
            source = consoleInput()
        )

    fun consoleFacade(commandExecutor: CommandExecutor = commandExecutor()): ConsoleFacade =
        ConsoleFacade(
            journalist = journalist,
            commandExecutor = commandExecutor
        )

}
