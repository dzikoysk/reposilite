package com.reposilite.token

import com.fasterxml.jackson.module.kotlin.readValue
import com.reposilite.ReposiliteObjectMapper
import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.api.ReposiliteCommand
import com.reposilite.token.api.AccessTokenDetails
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.SecretType.ENCRYPTED
import panda.std.Result
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlin.io.path.absolute
import kotlin.io.path.readText

@Command(name = "token-export", description = ["Export access tokens to file"])
internal class ExportTokensCommand(private val workingDirectory: Path, private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<file>", description = ["Path to destination JSON file"])
    lateinit var name: String

    override fun execute(context: CommandContext) {
        accessTokenFacade.getAccessTokens()
            .map { accessTokenFacade.getAccessTokenDetailsById(it.identifier)!! }
            .toList()
            .also { context.append("${it.size} token(s) found.") }
            .let { ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(it) }
            .let { Files.writeString(workingDirectory.resolve(name), it, TRUNCATE_EXISTING, CREATE) }
            .also { context.append("All tokens have been exported to ${it.absolute()} in JSON format.") }
    }

}

@Command(name = "token-import", description = ["Import access tokens from file"])
internal class ImportTokensCommand(private val workingDirectory: Path, private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<file>", description = ["Path to JSON file with access tokens"])
    lateinit var name: String

    override fun execute(context: CommandContext) {
        val file = workingDirectory.resolve(name)

        Result.attempt { ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER.readValue<List<AccessTokenDetails>>(file.readText()) }
            .onError {
                context.status = FAILED
                context.append("Cannot read JSON file due to: $it")
            }
            .orNull()
            ?.also { context.append("Importing ${it.size} token(s) from ${file.absolute()} file:") }
            ?.forEach { (accessToken, permissions, routes) ->
                val (accessTokenDto) = accessTokenFacade.createAccessToken(CreateAccessTokenRequest(
                    type = accessToken.identifier.type,
                    name = accessToken.name,
                    secretType = ENCRYPTED,
                    secret = accessToken.encryptedSecret
                ))

                permissions.forEach {
                    accessTokenFacade.addPermission(accessTokenDto.identifier, it)
                }

                routes.forEach {
                    accessTokenFacade.addRoute(accessTokenDto.identifier, Route(it.path, it.permission))
                }

                context.append("Access token '${accessTokenDto.name}' has been imported.")
            }
    }

}