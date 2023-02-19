package com.reposilite.plugin.migration

import com.reposilite.plugin.migration.specification.MigrationPluginSpecification
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.RoutePermission.WRITE
import org.assertj.core.api.Assertions.assertThat
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
        assertThat(Files.exists(schema)).isTrue
        assertThat(tokens.size).isEqualTo(3)

        val privateToken = tokens.first { it.accessToken.name == "private" }
        with (privateToken.routes.first()) {
            assertThat(path).isEqualTo("/private")
            assertThat(permission).isEqualTo(WRITE)
        }

        val wildcardToken = tokens.first { it.accessToken.name == "wildcard" }
        assertThat(wildcardToken.permissions).isEqualTo(setOf(MANAGER))
        assertThat(wildcardToken.routes).isEqualTo(repositories.map { Route("/$it/", READ) }.toSet())

        val adminToken = tokens.first { it.accessToken.name == "admin" }
        assertThat(adminToken.permissions).isEqualTo(setOf(MANAGER))
        assertThat(adminToken.routes).isEqualTo(setOf(Route("/", READ), Route("/", WRITE)))
    }

}