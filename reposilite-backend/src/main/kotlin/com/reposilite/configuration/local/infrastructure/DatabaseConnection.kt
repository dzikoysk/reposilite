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

package com.reposilite.configuration.local.infrastructure

import com.reposilite.configuration.local.LocalConfiguration.EmbeddedSQLDatabaseSettings
import com.reposilite.configuration.local.LocalConfiguration.StandardSQLDatabaseSettings
import com.reposilite.shared.extensions.loadCommandBasedConfiguration
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.Closeable
import java.io.File
import java.nio.file.Path
import java.sql.Connection.TRANSACTION_SERIALIZABLE
import kotlin.io.path.absolutePathString

data class DatabaseConnection(
    val databaseSource: HikariDataSource,
    val database: Database
) : Closeable {

    override fun close() =
        databaseSource.close()

}

object DatabaseConnectionFactory {

    private val DRIVER_MAPPING = mapOf(
        "jdbc:mariadb" to "org.mariadb.jdbc.Driver",
        "jdbc:mysql" to "com.mysql.cj.jdbc.Driver",
        "jdbc:postgresql" to "org.postgresql.Driver",
        "jdbc:sqlite" to "org.sqlite.JDBC",
        "jdbc:h2" to "org.h2.Driver",
    )

    fun createConnection(workingDirectory: Path, databaseConfiguration: String, databaseThreadPoolSize: Int): DatabaseConnection =
        when {
            databaseConfiguration.startsWith("mariadb") -> connectWithStandardDatabase(databaseConfiguration, "jdbc:mariadb", databaseThreadPoolSize)
            databaseConfiguration.startsWith("sqlite") -> connectWithEmbeddedDatabase(workingDirectory, databaseConfiguration, "jdbc:sqlite:%file%")
            /* Experimental implementations (not covered with integration tests) */
            databaseConfiguration.startsWith("mysql") -> connectWithStandardDatabase(databaseConfiguration, "jdbc:mysql", databaseThreadPoolSize)
            databaseConfiguration.startsWith("postgresql") -> connectWithStandardDatabase(databaseConfiguration, "jdbc:postgresql", databaseThreadPoolSize)
            databaseConfiguration.startsWith("h2") -> connectWithEmbeddedDatabase(workingDirectory, databaseConfiguration, "jdbc:h2:%file%;MODE=MYSQL")
            /* Raw */
            databaseConfiguration.startsWith("jdbc:") -> connectWithCustomJdbcUrl(databaseConfiguration, databaseThreadPoolSize)
            else -> throw RuntimeException("Unknown database: $databaseConfiguration")
        }

    private fun connectWithCustomJdbcUrl(jdbcUrl: String, databaseThreadPoolSize: Int): DatabaseConnection =
        createDataSource(jdbcUrl, databaseThreadPoolSize)
            .let { DatabaseConnection(it, Database.connect(it)) }

    private fun connectWithStandardDatabase(databaseConfiguration: String, dialect: String, databaseThreadPoolSize: Int): DatabaseConnection =
        with(loadCommandBasedConfiguration(StandardSQLDatabaseSettings(), databaseConfiguration).configuration) {
            createDataSource("$dialect://$host/$database", databaseThreadPoolSize, user, password)
                .let { DatabaseConnection(it, Database.connect(it)) }
        }

    private fun connectWithEmbeddedDatabase(workingDirectory: Path, databaseConfiguration: String, dialect: String): DatabaseConnection =
        with(loadCommandBasedConfiguration(EmbeddedSQLDatabaseSettings(), databaseConfiguration).configuration) {
            val databaseFile =
                if (temporary)
                    File.createTempFile("reposilite-database", ".db")
                        .also { it.deleteOnExit() }
                        .toPath()
                else
                    workingDirectory.resolve(fileName)

            createDataSource(dialect.replace("%file%", databaseFile.absolutePathString()), 1)
                .let { DatabaseConnection(it, Database.connect(it)) }
                .also { TransactionManager.manager.defaultIsolationLevel = TRANSACTION_SERIALIZABLE }
        }

    private fun createDataSource(url: String, threadPool: Int, username: String? = null, password: String? = null): HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                this.jdbcUrl = url
                this.driverClassName = resolveDriver(url)
                this.maximumPoolSize = threadPool
                username?.also { this.username = it }
                password?.also { this.password = it }
            }
        )

    private fun resolveDriver(jdbcUrl: String): String =
        DRIVER_MAPPING.entries
            .firstOrNull { (prefix, _) -> jdbcUrl.startsWith(prefix) }
            ?.value
            ?: throw RuntimeException("Unsupported JDBC driver for URL: $jdbcUrl")

}
