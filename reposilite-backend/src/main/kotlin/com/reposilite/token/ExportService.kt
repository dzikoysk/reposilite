/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.token

import com.fasterxml.jackson.module.kotlin.readValue
import com.reposilite.ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.api.ReposiliteCommand
import com.reposilite.token.api.AccessTokenDetails
import panda.std.Result
import panda.std.Result.supplyThrowing
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlin.io.path.absolute
import kotlin.io.path.readText

class ExportService {

    fun exportToFile(tokens: Collection<AccessTokenDetails>, toFile: Path): Path =
        Files.writeString(toFile, DEFAULT_OBJECT_MAPPER.writeValueAsString(tokens), TRUNCATE_EXISTING, CREATE)

    fun importFromFile(fromFile: Path): Result<Collection<AccessTokenDetails>, Exception> =
        supplyThrowing { DEFAULT_OBJECT_MAPPER.readValue<List<AccessTokenDetails>>(fromFile.readText()) }

}

@Command(name = "token-export", description = ["Export access tokens to file"])
internal class ExportTokensCommand(val workingDirectory: Path, val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<file>", description = ["Path to destination JSON file"])
    lateinit var name: String

    override fun execute(context: CommandContext) {
        workingDirectory.resolve(name)
            .let { accessTokenFacade.exportToFile(it) }
            .also {
                context.append("${accessTokenFacade.count()} token(s) found.")
                context.append("All tokens have been exported to ${it.absolute()} in JSON format.")
            }
    }

}

@Command(name = "token-import", description = ["Import access tokens from file"])
internal class ImportTokensCommand(val workingDirectory: Path, val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<file>", description = ["Path to JSON file with access tokens"])
    lateinit var name: String

    override fun execute(context: CommandContext) {
        workingDirectory.resolve(name)
            .let { accessTokenFacade.importFromFile(it) }
            .onError {
                context.status = FAILED
                context.append("Cannot read JSON file due to: $it")
                it.printStackTrace()
            }
            .orNull()
            ?.also { context.append("Importing ${it.size} token(s) from ${workingDirectory.resolve(name).absolute()} file:") }
            ?.map { accessTokenFacade.addAccessToken(it) }
            ?.forEach { context.append("Access token '${it.name}' has been imported.") }
    }

}
