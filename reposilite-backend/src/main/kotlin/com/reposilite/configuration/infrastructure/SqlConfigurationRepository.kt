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

package com.reposilite.configuration.infrastructure

import com.reposilite.configuration.ConfigurationRepository
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Suppress("RemoveRedundantQualifierName")
internal class SqlConfigurationRepository(private val database: Database) : ConfigurationRepository {

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
            } else {
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
        SettingsTable.selectAll()
            .where { SettingsTable.name eq name }
            .map { it[SettingsTable.content] }
            .firstOrNull()

    override fun findConfigurationUpdateDate(name: String): Instant? =
        transaction(database) {
            SettingsTable.selectAll()
                .where { SettingsTable.name eq name }
                .map { it[SettingsTable.updateDate] }
                .map { it.toInstant(ZoneOffset.UTC) }
                .firstOrNull()
        }

}
