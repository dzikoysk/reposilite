/*
 * Copyright (c) 2021 dzikoysk
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

package com.reposilite.settings.application

import com.reposilite.settings.api.LocalConfiguration.StandardSQLDatabaseSettings
import com.reposilite.settings.api.LocalConfiguration.EmbeddedSQLDatabaseSettings
import com.reposilite.shared.extensions.loadCommandBasedConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File
import java.nio.file.Path
import java.sql.Connection.TRANSACTION_SERIALIZABLE
import kotlin.io.path.absolutePathString

internal object DatabaseSourceFactory {

    fun createConnection(workingDirectory: Path, databaseConfiguration: String): Database =
        when {
            databaseConfiguration.startsWith("mysql") -> {
                connectWithCredentials(databaseConfiguration, "jdbc:mysql", "com.mysql.cj.jdbc.Driver")
            }
            databaseConfiguration.startsWith("sqlite") -> {
                val settings = loadCommandBasedConfiguration(EmbeddedSQLDatabaseSettings(), databaseConfiguration).configuration

                val database =
                    if (settings.temporary) {
                        val temporaryDatabase = File.createTempFile("reposilite-database", ".db")
                        temporaryDatabase.deleteOnExit()
                        Database.connect("jdbc:sqlite:${temporaryDatabase.absolutePath}", "org.sqlite.JDBC")
                    } else {
                        Database.connect("jdbc:sqlite:${workingDirectory.resolve(settings.fileName).absolutePathString()}", "org.sqlite.JDBC")
                    }

                TransactionManager.manager.defaultIsolationLevel = TRANSACTION_SERIALIZABLE
                database
            }
            /* Experimental implementations */
            databaseConfiguration.startsWith("postgresql") -> {
                connectWithCredentials(databaseConfiguration, "jdbc:postgresql", "org.postgresql.Driver")
            }
            databaseConfiguration.startsWith("h2") -> {
                val settings = loadCommandBasedConfiguration(EmbeddedSQLDatabaseSettings(), databaseConfiguration).configuration
                Database.connect("jdbc:h2:${workingDirectory.resolve(settings.fileName).absolutePathString()}", "org.h2.Driver")
            }
            else -> throw RuntimeException("Unknown database: $databaseConfiguration")
        }

    private fun connectWithCredentials(databaseConfiguration: String, dialect: String, driver:String): Database =
        with(loadCommandBasedConfiguration(StandardSQLDatabaseSettings(), databaseConfiguration).configuration) {
            Database.connect(
                url = "$dialect://${host}/${database}",
                driver = driver,
                user = user,
                password = password
            )
        }

}