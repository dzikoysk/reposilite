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