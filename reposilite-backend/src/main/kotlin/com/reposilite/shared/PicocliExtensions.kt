package com.reposilite.shared

import picocli.CommandLine

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