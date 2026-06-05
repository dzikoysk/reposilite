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

package com.reposilite.token.infrastructure

import com.reposilite.DatabaseMigrations.MIGRATION_004
import com.reposilite.configuration.local.infrastructure.DatabaseConnectionFactory
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.token.AccessToken
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission.READ
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path
import java.util.UUID

internal class SqlAccessTokenRepositoryTest {

    private val journalist = InMemoryLogger()

    @TempDir
    lateinit var workingDirectory: Path

    @ParameterizedTest
    @ValueSource(strings = ["sqlite --temporary", "h2 --temporary"])
    fun `deleting an access token removes its permissions and routes on all targets`(databaseConfiguration: String) {
        val connection = DatabaseConnectionFactory.createConnection(workingDirectory, databaseConfiguration, 1)

        connection.use {
            val repository = SqlAccessTokenRepository(it.database, journalist, emptyArray())

            val token = repository.saveAccessToken(AccessToken(name = "test", encryptedSecret = "secret"))
            repository.addPermission(token.identifier, MANAGER)
            repository.addRoute(token.identifier, Route("/private", READ))

            // given: the dependent rows are persisted
            assertThat(countPermissions(it.database)).isEqualTo(1)
            assertThat(countRoutes(it.database)).isEqualTo(1)

            // when: the token is deleted
            repository.deleteAccessToken(token.identifier)

            // then: the dependent rows are removed alongside it (foreign key cascade)
            assertThat(countPermissions(it.database)).isZero()
            assertThat(countRoutes(it.database)).isZero()
        }
    }

    @Test
    fun `migration 004 removes orphaned permissions and routes`() {
        // given: a SQLite database without foreign key enforcement (the legacy state that leaked rows)
        val databaseFile = workingDirectory.resolve("orphans.db").toAbsolutePath()
        val database = Database.connect("jdbc:sqlite:$databaseFile", driver = "org.sqlite.JDBC")

        val repository = SqlAccessTokenRepository(database, journalist, emptyArray())
        val token = repository.saveAccessToken(AccessToken(name = "valid", encryptedSecret = "secret"))
        repository.addPermission(token.identifier, MANAGER)
        repository.addRoute(token.identifier, Route("/valid", READ))

        // and: permissions and routes pointing at a token that no longer exists
        transaction(database) {
            val ghost = EntityID(token.identifier.value + 1000, AccessTokenTable)
            PermissionToAccessTokenTable.insert {
                it[accessTokenId] = ghost
                it[permission] = MANAGER.identifier
            }
            PermissionToRouteTable.insert {
                it[accessTokenId] = ghost
                it[routeId] = UUID.nameUUIDFromBytes("/ghost".toByteArray())
                it[route] = "/ghost"
                it[permission] = READ.identifier
            }
        }
        assertThat(countPermissions(database)).isEqualTo(2)
        assertThat(countRoutes(database)).isEqualTo(2)

        // when: migration 004 runs
        SqlAccessTokenRepository(database, journalist, arrayOf(MIGRATION_004))

        // then: only the rows of the existing token remain
        assertThat(countPermissions(database)).isEqualTo(1)
        assertThat(countRoutes(database)).isEqualTo(1)
        assertThat(repository.findAccessTokenPermissionsById(token.identifier)).containsExactly(MANAGER)
        assertThat(repository.findAccessTokenRoutesById(token.identifier)).containsExactly(Route("/valid", READ))
    }

    private fun countPermissions(database: Database): Long =
        transaction(database) { PermissionToAccessTokenTable.selectAll().count() }

    private fun countRoutes(database: Database): Long =
        transaction(database) { PermissionToRouteTable.selectAll().count() }

}
