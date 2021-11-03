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

package com.reposilite.statistics.infrastructure

import com.reposilite.shared.firstAndMap
import com.reposilite.statistics.StatisticsRepository
import com.reposilite.statistics.StatisticsRepository.Companion.MAX_IDENTIFIER_LENGTH
import com.reposilite.statistics.api.Identifier
import com.reposilite.statistics.api.IncrementResolvedRequest
import net.dzikoysk.exposed.upsert.upsert
import net.dzikoysk.exposed.upsert.withUnique
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID

@Suppress("RemoveRedundantQualifierName")
internal class SqlStatisticsRepository(private val database: Database) : StatisticsRepository {

    companion object {
        const val INET6_ADDRESS_LENGTH = 45
    }

    internal object StatisticsIdentifierTable : Table("statistics_identifier") {
        val id = uuid("identifier_id")
        val repository = varchar("repository", 100)
        val gav = varchar("gav", MAX_IDENTIFIER_LENGTH)
        override val primaryKey = PrimaryKey(id)
    }

    internal object StatisticsIdentifierResolvedTable : IntIdTable("statistics_identifier_resolved") {
        val identifierId = reference("identifier_id", StatisticsIdentifierTable.id, onDelete = CASCADE, onUpdate = CASCADE)
        val date = date("date")
        val count = long("count")

        val uniqueIdentifierIdWithDate = withUnique("uq_identifier_id_date", identifierId, date)
    }

    /*
    internal object StatisticsIdentifierDeployedTable : IntIdTable("statistics_identifier_deployed") {
        val identifierId = reference("identifier_id", StatisticsIdentifierTable.id, onDelete = CASCADE, onUpdate = CASCADE)
        val date = datetime("time")
        val by = varchar("by", MAX_TOKEN_NAME + INET6_ADDRESS_LENGTH + 1)
    }
     */

    init {
        transaction(database) {
            SchemaUtils.create(StatisticsIdentifierTable, StatisticsIdentifierResolvedTable, /* StatisticsIdentifierDeployedTable */)
            SchemaUtils.addMissingColumnsStatements(StatisticsIdentifierTable, StatisticsIdentifierResolvedTable, /* StatisticsIdentifierDeployedTable */)
        }
    }

    override fun incrementResolvedRequests(requests: Map<Identifier, Long>) =
        transaction(database) {
            requests.forEach { (identifier, count) ->
                StatisticsIdentifierResolvedTable.upsert(conflictIndex = StatisticsIdentifierResolvedTable.uniqueIdentifierIdWithDate,
                    insertBody = {
                        it[StatisticsIdentifierResolvedTable.identifierId] = findOrCreateIdentifierId(identifier)
                        it[StatisticsIdentifierResolvedTable.date] = LocalDate.now()
                        it[StatisticsIdentifierResolvedTable.count] = count
                    },
                    updateBody = {
                        with(SqlExpressionBuilder) {
                            it[StatisticsIdentifierResolvedTable.count] = StatisticsIdentifierResolvedTable.count + count
                        }
                    }
                )
            }
        }

    /*
    override fun recordDeployed(identifier: String, by: String) {
        transaction(database) {
            StatisticsIdentifierDeployedTable.insert {
                it[StatisticsIdentifierDeployedTable.identifierId] = findOrCreateIdentifierId(identifier)
                it[StatisticsIdentifierDeployedTable.date] = LocalDateTime.now()
                it[StatisticsIdentifierDeployedTable.by] = by
            }
        }
    }
     */

    private fun findOrCreateIdentifierId(identifier: Identifier): UUID =
        findIdentifier(identifier) ?: createIdentifier(identifier)

    private fun createIdentifier(identifier: Identifier): UUID  =
        identifier.toUUID().also { id ->
            StatisticsIdentifierTable.insert {
                it[StatisticsIdentifierTable.id] = id
                it[StatisticsIdentifierTable.repository] = identifier.repository
                it[StatisticsIdentifierTable.gav] = identifier.gav
            }
        }

    private fun findIdentifier(identifier: Identifier): UUID? =
        with(identifier.toUUID()) {
            StatisticsIdentifierTable.select { StatisticsIdentifierTable.id eq this@with }
                .firstOrNull()
                ?.let { it[StatisticsIdentifierTable.id] }
        }

    override fun findResolvedRequestsByPhrase(repository: String, phrase: String, limit: Int): List<Identifier> =
        transaction(database) {  }

    override fun countResolvedRecords(): Long =
        transaction(database) {
            with (StatisticsIdentifierResolvedTable.count.sum()) {
                StatisticsIdentifierResolvedTable.slice(this).selectAll().firstAndMap { it[this] }
            }
            ?: 0
        }

    override fun countUniqueResolvedRequests(): Long =
        transaction(database) {
            StatisticsIdentifierResolvedTable.selectAll()
                .groupBy(StatisticsIdentifierResolvedTable.identifierId)
                .count()
        }

    private fun String.toUUID(): UUID =
        UUID.nameUUIDFromBytes(this.encodeToByteArray())

}