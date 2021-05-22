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
package org.panda_lang.reposilite.console

import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import org.panda_lang.reposilite.ReposiliteConstants
import org.panda_lang.reposilite.failure.FailureFacade
import org.panda_lang.utilities.commons.function.Result
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.MissingParameterException
import picocli.CommandLine.UnmatchedArgumentException
import java.io.InputStream
import java.util.function.Consumer

@Command(name = "", version = ["Reposilite " + ReposiliteConstants.VERSION])
internal class Console(
    private val journalist: Journalist,
    private val failureFacade: FailureFacade,
    private val source: InputStream
) : Journalist {

    private val consoleThread = ConsoleThread(this, source, failureFacade)
    private val commandExecutor = CommandLine(this)

    fun executeLocalCommand(command: String): Boolean {
        logger.info("")
        val status = execute(command) { logger.info(it) }
        logger.info("")
        return status
    }

    fun execute(command: String, outputConsumer: Consumer<String>): Boolean {
        val response = executeCommand(command)

        for (entry in if (response.isOk) response.get() else response.error) {
            for (line in entry.replace(System.lineSeparator(), "\n").split("\n").toTypedArray()) {
                outputConsumer.accept(line)
            }
        }
        return response.isOk
    }

    private fun executeCommand(command: String): Result<List<String>, List<String>> {
        val processedCommand = command.trim()

        if (processedCommand.isEmpty()) {
            return Result.error(emptyList())
        }

        val response: MutableList<String> = ArrayList()

        return try {
            val parseResult = commandExecutor.parseArgs(*processedCommand.split(" ").toTypedArray())

            val commandObject = parseResult.subcommand().commandSpec().userObject() as? ReposiliteCommand
                ?: return Result.error(listOf(commandExecutor.usageMessage))

            if (commandObject.execute(response)) Result.ok(response) else Result.error(response)
        }
        catch (unmatchedArgumentException: UnmatchedArgumentException) {
            Result.error(listOf("Unknown command $processedCommand"))
        }
        catch (missingParameterException: MissingParameterException) {
            response.add(missingParameterException.message.toString())
            response.add("")
            response.add(missingParameterException.commandLine.usageMessage)
            Result.error(response)
        }
    }

    fun registerCommand(command: ReposiliteCommand): CommandLine =
        commandExecutor.addSubcommand(command)

    fun getCommands(): Map<String, CommandLine> =
        commandExecutor.subcommands

    fun hook() =
        consoleThread.start()

    fun stop() =
        consoleThread.interrupt()

    override fun getLogger(): Logger =
        journalist.logger

}