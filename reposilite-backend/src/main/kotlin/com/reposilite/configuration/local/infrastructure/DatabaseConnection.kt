/*
 * Copyright (c) 2022 dzikoysk
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

    fun createConnection(workingDirectory: Path, databaseConfiguration: String, databaseThreadPoolSize: Int): DatabaseConnection =
        when {
            databaseConfiguration.startsWith("mysql") -> connectWithStandardDatabase(databaseConfiguration, "jdbc:mysql", "com.mysql.cj.jdbc.Driver", databaseThreadPoolSize)
            databaseConfiguration.startsWith("sqlite") -> connectWithEmbeddedDatabase(workingDirectory, databaseConfiguration, "org.sqlite.JDBC", "jdbc:sqlite:%file%")
            /* Experimental implementations (not covered with integration tests) */
            databaseConfiguration.startsWith("postgresql") -> connectWithStandardDatabase(databaseConfiguration, "jdbc:postgresql", "org.postgresql.Driver", databaseThreadPoolSize)
            databaseConfiguration.startsWith("h2") -> connectWithEmbeddedDatabase(workingDirectory, databaseConfiguration, "org.h2.Driver", "jdbc:h2:%file%;MODE=MYSQL")
            else -> throw RuntimeException("Unknown database: $databaseConfiguration")
        }

    private fun connectWithStandardDatabase(databaseConfiguration: String, dialect: String, driver: String, databaseThreadPoolSize: Int): DatabaseConnection =
        with(loadCommandBasedConfiguration(StandardSQLDatabaseSettings(), databaseConfiguration).configuration) {
            createDataSource(driver, "$dialect://$host/$database", databaseThreadPoolSize, user, password)
                .let { DatabaseConnection(it, Database.connect(it)) }
        }

    private fun connectWithEmbeddedDatabase(workingDirectory: Path, databaseConfiguration: String, driver: String, dialect: String): DatabaseConnection =
        with(loadCommandBasedConfiguration(EmbeddedSQLDatabaseSettings(), databaseConfiguration).configuration) {
            val databaseFile =
                if (temporary)
                    File.createTempFile("reposilite-database", ".db")
                        .also { it.deleteOnExit() }
                        .toPath()
                else
                    workingDirectory.resolve(fileName)

            createDataSource(driver, dialect.replace("%file%", databaseFile.absolutePathString()), 1)
                .let { DatabaseConnection(it, Database.connect(it)) }
                .also { TransactionManager.manager.defaultIsolationLevel = TRANSACTION_SERIALIZABLE }
        }

    private fun createDataSource(driver: String, url: String, threadPool: Int, username: String? = null, password: String? = null): HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                this.jdbcUrl = url
                this.driverClassName = driver
                this.maximumPoolSize = threadPool
                username?.also { this.username = it }
                password?.also { this.password = it }
            }
        )

}
