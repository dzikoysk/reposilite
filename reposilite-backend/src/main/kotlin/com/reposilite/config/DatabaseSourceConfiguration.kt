package com.reposilite.config

import com.reposilite.config.Configuration.MySqlDatabaseSettings
import com.reposilite.config.Configuration.SQLiteDatabaseSettings
import com.reposilite.shared.loadCommandBasedConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.nio.file.Path
import java.sql.Connection

object DatabaseSourceConfiguration {

    fun createConnection(workingDirectory: Path, databaseConfiguration: String): Database {
        return when {
            databaseConfiguration.startsWith("sqlite") -> {
                val settings = loadCommandBasedConfiguration(SQLiteDatabaseSettings(), databaseConfiguration).configuration

                val database =
                    if (settings.inMemory)
                        Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared", "org.sqlite.JDBC")
                    else
                        Database.connect("jdbc:sqlite:${workingDirectory.resolve(settings.fileName)}", "org.sqlite.JDBC")

                TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                database
            }
            databaseConfiguration.startsWith("mysql") -> {
                val settings = loadCommandBasedConfiguration(MySqlDatabaseSettings(), databaseConfiguration).configuration

                Database.connect(
                    url = "jdbc:mysql://${settings.host}/${settings.database}",
                    driver = "com.mysql.jdbc.Driver",
                    user = settings.user,
                    password = settings.password
                )
            }
            else -> throw RuntimeException("Unknown database: $databaseConfiguration")
        }
    }

}