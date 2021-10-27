package com.reposilite.settings.infrastructure

import com.reposilite.settings.SettingsRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Suppress("RemoveRedundantQualifierName")
internal class SqlSettingsRepository(private val database: Database) : SettingsRepository {

    internal object SettingsTable : Table("settings") {
        val name = varchar("name", 32).uniqueIndex()
        val updateDate = datetime("updateDate")
        val content = text("content")
    }

    init {
        transaction(database) {
            SchemaUtils.create(SettingsTable)
            SchemaUtils.addMissingColumnsStatements(SettingsTable)
        }
    }

    override fun saveConfiguration(name: String, configuration: String) {
        transaction(database) {
            if (findSharedConfigurationContent(name) == null) {
                SettingsTable.insert {
                    it[SettingsTable.name] = name
                    it[SettingsTable.updateDate] = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    it[SettingsTable.content] = configuration
                }
            }
            else {
                SettingsTable.update(where = { SettingsTable.name eq name }, body = {
                    it[SettingsTable.updateDate] = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    it[SettingsTable.content] = configuration
                })
            }
        }
    }

    override fun findConfiguration(name: String): String? =
        transaction(database) {
            findSharedConfigurationContent(name)
        }

    private fun findSharedConfigurationContent(name: String): String? =
        SettingsTable.select { SettingsTable.name eq name }
            .map { it[SettingsTable.content] }
            .firstOrNull()

    override fun findConfigurationUpdateDate(name: String): Instant? =
        transaction(database) {
            SettingsTable.select { SettingsTable.name eq name }
                .map { it[SettingsTable.updateDate] }
                .map { it.toInstant(ZoneOffset.UTC) }
                .firstOrNull()
        }

}