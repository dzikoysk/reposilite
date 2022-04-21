package com.reposilite.plugin.migration

import com.reposilite.plugin.migration.specification.MigrationPluginSpecification
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
        val schema = workingDirectory().resolve ("tokens.json")
        assertTrue(Files.exists(schema))
    }

}