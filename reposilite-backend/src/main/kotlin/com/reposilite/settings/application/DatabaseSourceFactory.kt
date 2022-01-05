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

import com.reposilite.settings.api.LocalConfiguration.MySqlDatabaseSettings
import com.reposilite.settings.api.LocalConfiguration.SQLiteDatabaseSettings
import com.reposilite.shared.extensions.loadCommandBasedConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteConfig.JournalMode.WAL
import org.sqlite.SQLiteConfig.SynchronousMode.NORMAL
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection.TRANSACTION_SERIALIZABLE
import kotlin.io.path.absolutePathString

internal object DatabaseSourceFactory {

    fun createConnection(workingDirectory: Path, databaseConfiguration: String): Database =
        when {
            databaseConfiguration.startsWith("sqlite") -> {
                val settings = loadCommandBasedConfiguration(SQLiteDatabaseSettings(), databaseConfiguration).configuration

                val configuration = SQLiteConfig().also {
                    it.setSynchronous(NORMAL)
                    it.setJournalMode(WAL)
                    it.busyTimeout = 3000
                }

                val database =
                    if (settings.temporary) {
                        val temporaryDatabase = File.createTempFile("reposilite-database", ".db")
                        temporaryDatabase.deleteOnExit()
                        Database.connect("jdbc:sqlite:${temporaryDatabase.absolutePath}", "org.sqlite.JDBC", setupConnection = { configuration.apply(it) })
                    } else {
                        val databaseFile = workingDirectory.resolve(settings.fileName)
                        if (Files.notExists(databaseFile)) Files.createFile(databaseFile)
                        Database.connect("jdbc:sqlite:${databaseFile.absolutePathString()}", "org.sqlite.JDBC", setupConnection = { configuration.apply(it) })
                    }

                TransactionManager.manager.defaultIsolationLevel = TRANSACTION_SERIALIZABLE
                database
            }
            databaseConfiguration.startsWith("mysql") -> {
                val settings = loadCommandBasedConfiguration(MySqlDatabaseSettings(), databaseConfiguration).configuration

                Database.connect(
                    url = "jdbc:mysql://${settings.host}/${settings.database}",
                    driver = "com.mysql.cj.jdbc.Driver",
                    user = settings.user,
                    password = settings.password
                )
            }
            else -> throw RuntimeException("Unknown database: $databaseConfiguration")
        }

}