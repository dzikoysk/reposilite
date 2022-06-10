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

package com.reposilite.shared.extensions

import com.reposilite.VERSION
import panda.std.Result
import panda.std.asSuccess
import picocli.CommandLine
import java.util.TreeSet

abstract class Validator : Runnable {
    override fun run() { }
}

internal data class CommandConfiguration<VALUE>(
    val name: String,
    val configuration: VALUE
)

internal fun <CONFIGURATION : Runnable> loadCommandBasedConfiguration(configuration: CONFIGURATION, description: String): CommandConfiguration<CONFIGURATION> =
    description.split(" ", limit = 2)
        .let { CommandConfiguration(it[0], it.getOrElse(1) { "" }) }
        .also { CommandLine(configuration).execute(*splitArguments(it.configuration)) }
        .let { CommandConfiguration(it.name, configuration) }
        .also { it.configuration.run() }

private fun splitArguments(args: String): Array<String> =
    if (args.isEmpty()) arrayOf() else args.split(" ").toTypedArray()

internal fun createCommandHelp(commands: Map<String, CommandLine>, requestedCommand: String): Result<List<String>, String> {
    if (requestedCommand.isNotEmpty()) {
        return commands[requestedCommand]
            ?.let { listOf(it.usageMessage).asSuccess() }
            ?: error("Unknown command '$requestedCommand'")
    }

    val uniqueCommands: MutableSet<CommandLine> = TreeSet(Comparator.comparing { it.commandName })
    uniqueCommands.addAll(commands.values)

    val response = mutableListOf("Reposilite $VERSION Commands:")

    uniqueCommands
        .forEach { command ->
            val specification = command.commandSpec

            response.add("  " + command.commandName
                    + (if (specification.args().isEmpty()) "" else " ")
                    + specification.args().joinToString(separator = " ", transform = {
                            if (it.isOption) {
                                var option = ""
                                if (!it.required()) option += "["
                                option += "--"
                                option += if (it.paramLabel().startsWith("<")) it.paramLabel().substring(1).dropLast(1) else it.paramLabel()
                                if (it.type() != Boolean::class.javaPrimitiveType) option += "=<value>"
                                if (!it.required()) option += "]"
                                option
                            }
                            else it.paramLabel()
                    })
                    + " - ${specification.usageMessage().description().joinToString(". ")}"
            )
        }

    return response.asSuccess()
}