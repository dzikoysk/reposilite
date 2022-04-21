package com.reposilite.plugin.migration

import com.charleskorn.kaml.Yaml
import com.reposilite.maven.MavenFacade
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.facade
import com.reposilite.plugin.parameters
import com.reposilite.token.AccessToken
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.ExportService
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission
import com.reposilite.token.api.AccessTokenDetails
import net.dzikoysk.cdn.KCdnFactory
import net.dzikoysk.cdn.loadAs
import net.dzikoysk.cdn.source.Source
import org.panda_lang.reposilite.auth.TokenCollection
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.math.log

@Plugin(name = "migration", dependencies = ["maven"])
class MigrationPlugin : ReposilitePlugin() {

    override fun initialize(): Facade? {
        val workingDirectory = parameters().workingDirectory
        val mavenFacade = facade<MavenFacade>()

        migrateTokens(workingDirectory, mavenFacade.getAllRepositoryNames())

        return null
    }

    fun migrateTokens(workingDirectory: Path, repositories: Collection<String>): Collection<AccessTokenDetails>? {
        val tokensFile = workingDirectory.resolve("tokens.dat")

        if (Files.notExists(tokensFile)) {
            logger.warn("[Migration] 'tokens.dat' file not found in working directory, there is nothing that migration plugin can do.")
            return null
        }

        logger.warn("[Migration] Reposilite 2.x 'tokens.dat' file found, the migration procedure has started.")
        val tokenCollection = Yaml.default.decodeFromStream(TokenCollection.serializer(), tokensFile.readBytes().inputStream())
        logger.warn("[Migration] ${tokenCollection.tokens.size} token(s) found in 'tokens.dat' file.")

        val migratedTokens = tokenCollection.tokens.map { token ->
            val routePermissions = token.permissions
                .toCharArray()
                .map { shortcut -> RoutePermission.findRoutePermissionByShortcut(shortcut.toString()).orNull() }
                .filterNotNull()
                .toSet()

            AccessTokenDetails(
                accessToken = AccessToken(
                    name = token.alias,
                    encryptedSecret = token.token
                ),
                permissions = token.permissions
                    .toCharArray()
                    .map { shortcut -> AccessTokenPermission.findAccessTokenPermissionByShortcut(shortcut.toString()) }
                    .filterNotNull()
                    .toSet(),
                routes =
                    if (token.path.startsWith("*"))
                        routePermissions
                            .flatMap { permission -> repositories.map { permission to it } }
                            .map { (permission, repository) -> Route(token.path.replace("*", "/$repository"), permission) }
                            .toSet()
                    else
                        routePermissions
                            .map { Route(token.path, it) }
                            .toSet()

            )
        }

        val exportService = ExportService(workingDirectory)
        exportService.exportToFile(migratedTokens, "tokens.json")
        logger.warn("[Migration] ${migratedTokens.size} token(s) have been exported to 'tokens.json' file.")
        logger.warn("[Migration] Run 'token-import tokens.json' command to import those tokens.")
        logger.warn("[Migration] This plugin is no longer needed, you can remove it.")

        return migratedTokens
    }

}