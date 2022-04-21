package com.reposilite.token

import com.fasterxml.jackson.module.kotlin.readValue
import com.reposilite.ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.api.ReposiliteCommand
import com.reposilite.token.api.AccessTokenDetails
import panda.std.Result
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlin.io.path.absolute
import kotlin.io.path.readText

class ExportService(val workingDirectory: Path) {

    fun exportToFile(tokens: Collection<AccessTokenDetails>, toFile: String): Path {
        val scheme = DEFAULT_OBJECT_MAPPER.writeValueAsString(tokens)
        val destination = workingDirectory.resolve(toFile)
        Files.writeString(destination, scheme, TRUNCATE_EXISTING, CREATE)
        return destination
    }

    fun importFromFile(fromFile: String): Result<Pair<Path, Collection<AccessTokenDetails>>, Exception> =
        Result.attempt {
            val source = workingDirectory.resolve(fromFile)
            source to DEFAULT_OBJECT_MAPPER.readValue<List<AccessTokenDetails>>(source.readText())
        }

}

@Command(name = "token-export", description = ["Export access tokens to file"])
internal class ExportTokensCommand(val accessTokenFacade: AccessTokenFacade, val exportService: ExportService) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<file>", description = ["Path to destination JSON file"])
    lateinit var name: String

    override fun execute(context: CommandContext) {
        accessTokenFacade.getAccessTokens()
            .map { accessTokenFacade.getAccessTokenDetailsById(it.identifier)!! }
            .toList()
            .also { context.append("${it.size} token(s) found.") }
            .let { exportService.exportToFile(it, name) }
            .also { context.append("All tokens have been exported to ${it.absolute()} in JSON format.") }
    }

}

@Command(name = "token-import", description = ["Import access tokens from file"])
internal class ImportTokensCommand(val accessTokenFacade: AccessTokenFacade, val exportService: ExportService) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<file>", description = ["Path to JSON file with access tokens"])
    lateinit var name: String

    override fun execute(context: CommandContext) {
        exportService.importFromFile(name)
            .onError {
                context.status = FAILED
                context.append("Cannot read JSON file due to: $it")
            }
            .orNull()
            ?.also { (source, tokens) -> context.append("Importing ${tokens.size} token(s) from ${source.absolute()} file:") }
            ?.second
            ?.map { accessTokenFacade.addAccessToken(it) }
            ?.forEach { context.append("Access token '${it.name}' has been imported.") }
    }

}