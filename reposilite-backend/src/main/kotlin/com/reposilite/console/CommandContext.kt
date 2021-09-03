package com.reposilite.console

enum class CommandStatus {
    SUCCEEDED,
    FAILED
}

class CommandContext {

    var status = CommandStatus.SUCCEEDED
    private val output = mutableListOf<String>()

    fun append(outputMessage: String): CommandContext =
        also { output.add(outputMessage) }

    fun appendAll(messages: List<String>): CommandContext =
        also { output.addAll(messages) }

    fun output(): List<String> =
        output

}