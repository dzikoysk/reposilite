/*
 * Copyright (c) 2022 dzikoysk
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

import com.reposilite.status.FailureFacade
import java.io.InputStream
import java.util.*

internal class ConsoleThread(
    private val commandExecutor: CommandExecutor,
    private val source: InputStream,
    private val failureFacade: FailureFacade
) : Thread() {

    init {
        name = "Reposilite | Console Thread"
        isDaemon = true
    }

    override fun run() {
        val input = Scanner(source)

        if (!input.hasNextLine()) {
            commandExecutor.logger.warn("Interactive CLI is not available in current environment.")
            commandExecutor.logger.warn("Solution for Docker users: https://docs.docker.com/engine/reference/run/#foreground")
            return
        }

        do {
            val command = input.nextLine().trim()

            if (command.isEmpty()) {
                continue
            }

            runCatching {
                commandExecutor.logger.info("")
                commandExecutor.execute(command)
                commandExecutor.logger.info("")
            }.onFailure {
                failureFacade.throwException("Command: $command", it)
            }
        } while (!isInterrupted && input.hasNextLine())
    }

}
