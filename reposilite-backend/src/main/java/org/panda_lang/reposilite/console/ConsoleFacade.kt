package org.panda_lang.reposilite.console

import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger

class ConsoleFacade internal constructor(
    private val journalist: Journalist,
    private val console: Console
) : Journalist {

    fun registerCommand(command: ReposiliteCommand) {
        console.registerCommand(command)
    }

    override fun getLogger(): Logger = journalist.logger

}