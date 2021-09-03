package com.reposilite.shared

import com.reposilite.VERSION
import panda.std.Result
import panda.std.asSuccess
import panda.utilities.text.Joiner
import picocli.CommandLine
import java.util.TreeSet

fun <CONFIGURATION : Runnable> loadCommandBasedConfiguration(configuration: CONFIGURATION, description: String): Pair<String, CONFIGURATION> =
    description.split(" ", limit = 2)
        .let { Pair(it[0], it.getOrNull(1) ?: "") }
        .also {
            val commandLine = CommandLine(configuration)

            val args =
                if (it.second.isEmpty())
                    arrayOf()
                else
                    it.second.split(" ").toTypedArray()

            commandLine.execute(*args)
        }
        .let { Pair(it.first, configuration) }
        .also { it.second.run() }

fun createCommandHelp(commands: Map<String, CommandLine>, requestedCommand: String): Result<List<String>, String> {
    if (requestedCommand.isNotEmpty()) {
        return commands[requestedCommand]
            ?.let { listOf(it.usageMessage).asSuccess() }
            ?: error("Unknown command '$requestedCommand'")
    }

    val uniqueCommands: MutableSet<CommandLine> = TreeSet(Comparator.comparing { it.commandName })
    uniqueCommands.addAll(commands.values)

    val response = mutableListOf("Reposilite $VERSION Commands:")

    for (command in uniqueCommands) {
        val specification = command.commandSpec

        response.add("  " + command.commandName
                + " " + Joiner.on(" ").join(specification.args()) { obj -> obj.paramLabel() }
                + " - " + Joiner.on(". ").join(*specification.usageMessage().description()))
    }

    return response.asSuccess()
}