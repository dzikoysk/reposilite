package com.reposilite.plugin.migration

import com.reposilite.plugin.migration.specification.MigrationPluginSpecification
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.RoutePermission.WRITE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

internal class MigrationPluginTest : MigrationPluginSpecification() {

    @Test
    fun `should migrate old yaml scheme to the new json scheme`() {
        // given: a tokens.dat file and a list of repositories
        val tokensFile = workingDirectory().resolve("tokens.dat")
        Files.write(tokensFile, resource(test = "should migrate old yaml scheme to the new json scheme", file = "tokens.dat").toByteArray())
        val repositories = setOf("releases", "snapshots", "private")

        // when: migration plugin will proceed migration
        val tokens = migrationPlugin.migrateTokens(workingDirectory(), repositories)!!

        // then: a valid json scheme with up-to-date tokens should be generated
        val schema = workingDirectory().resolve("tokens.json")
        assertTrue(Files.exists(schema))
        assertEquals(3, tokens.size)

        val privateToken = tokens.first { it.accessToken.name == "private" }
        with (privateToken.routes.first()) {
            assertEquals("/private", path)
            assertEquals(WRITE, permission)
        }

        val wildcardToken = tokens.first { it.accessToken.name == "wildcard" }
        assertEquals(setOf(MANAGER), wildcardToken.permissions)
        assertEquals(
            repositories.map { Route("/$it/", READ) }.toSet(),
            wildcardToken.routes
        )

        val adminToken = tokens.first { it.accessToken.name == "admin" }
        assertEquals(setOf(MANAGER), adminToken.permissions)
        assertEquals(setOf(Route("/", READ), Route("/", WRITE)), adminToken.routes)
    }

}