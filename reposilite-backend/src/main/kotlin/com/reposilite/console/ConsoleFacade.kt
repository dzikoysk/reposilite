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

import com.reposilite.console.api.ExecutionResponse
import com.reposilite.console.api.ReposiliteCommand
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.BAD_REQUEST
import panda.std.Result
import panda.std.asSuccess
import panda.utilities.StringUtils
import picocli.CommandLine

const val MAX_COMMAND_LENGTH = 1024

class ConsoleFacade internal constructor(
    private val journalist: Journalist,
    internal val console: Console
) : Journalist {

    fun executeCommand(command: String): Result<ExecutionResponse, ErrorResponse> {
        if (StringUtils.isEmpty(command)) {
            return errorResponse(BAD_REQUEST, "Missing command")
        }

        if (command.length > MAX_COMMAND_LENGTH) {
            return errorResponse(BAD_REQUEST, "The given command exceeds allowed length (${command.length} > $MAX_COMMAND_LENGTH)")
        }

        return console.execute(command).asSuccess()
    }

    fun registerCommand(command: ReposiliteCommand): CommandLine =
        console.registerCommand(command)

    fun getCommands(): Map<String, CommandLine> =
        console.getCommands()

    override fun getLogger(): Logger =
        journalist.logger

}