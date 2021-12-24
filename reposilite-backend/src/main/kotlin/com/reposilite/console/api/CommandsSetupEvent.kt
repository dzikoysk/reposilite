package com.reposilite.console.api

import com.reposilite.plugin.api.Event

class CommandsSetupEvent : Event {

    private val commands = mutableListOf<ReposiliteCommand>()

    fun registerCommand(command: ReposiliteCommand) {
        commands.add(command)
    }

    fun getCommands(): Collection<ReposiliteCommand> =
        commands

}