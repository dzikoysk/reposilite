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

import com.reposilite.failure.FailureFacade
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.util.Scanner

internal class ConsoleThread(
    private val console: Console,
    private val source: InputStream,
    private val dispatcher: CoroutineDispatcher,
    private val failureFacade: FailureFacade
) : Thread() {

    init {
        name = "Reposilite | Console Thread"
        isDaemon = true
    }

    override fun run() {
        val input = Scanner(source)

        if (!input.hasNextLine()) {
            console.logger.warn("Interactive CLI is not available in current environment.")
            console.logger.warn("Solution for Docker users: https://docs.docker.com/engine/reference/run/#foreground")
            return
        }

        /*
        val reader = LineReaderBuilder.builder().build();
        val prompt = "$"

        while (!isInterrupted && input.hasNextLine()) {
            try {
                val line = reader.readLine(prompt)

                runBlocking {
                    console.logger.info("")
                    console.execute(line)
                    console.logger.info("")
                }
            } catch (e: UserInterruptException) {
                // Ignore
            } catch (e: EndOfFileException) {
                return
            }
        }
        */

        do {
            val command = input.nextLine().trim()

            if (command.isEmpty()) {
                continue
            }

            runCatching {
                runBlocking {
                    console.logger.info("")
                    console.execute(command)
                    console.logger.info("")
                }
            }.onFailure {
                failureFacade.throwException("Command: $command", it)
            }
        } while (!isInterrupted && input.hasNextLine())
    }

}