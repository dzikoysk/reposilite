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

import com.reposilite.maven.api.GAV_MAX_LENGTH
import com.reposilite.maven.api.Identifier
import com.reposilite.maven.api.REPOSITORY_NAME_MAX_LENGTH
import com.reposilite.shared.extensions.and
import com.reposilite.shared.extensions.firstAndMap
import com.reposilite.statistics.StatisticsRepository
import com.reposilite.statistics.api.ResolvedEntry
import net.dzikoysk.exposed.upsert.upsert
import net.dzikoysk.exposed.upsert.withUnique
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID

@Suppress("RemoveRedundantQualifierName")
internal class SqlStatisticsRepository(private val database: Database) : StatisticsRepository {

    private object IdentifierTable : Table("statistics_identifier") {
        val id = uuid("identifier_id")
        val repository = varchar("repository", REPOSITORY_NAME_MAX_LENGTH).index("idx_statistics_identifier_repository")
        val gav = varchar("gav", GAV_MAX_LENGTH)

        override val primaryKey = PrimaryKey(id)
    }

    private object ResolvedTable : IntIdTable("statistics_resolved_identifier") {
        val identifierId = reference("identifier_id", IdentifierTable.id, onDelete = CASCADE, onUpdate = CASCADE)
        val date = date("date")
        val count = long("count")

        val uniqueIdentifierIdWithDate = withUnique("uq_statistics_resolved_identifier_identifier_id_date", identifierId, date)
    }

    init {
        transaction(database) {
            SchemaUtils.create(IdentifierTable, ResolvedTable)
            SchemaUtils.addMissingColumnsStatements(IdentifierTable, ResolvedTable)
        }
    }

    override fun incrementResolvedRequests(requests: Map<Identifier, Long>, date: LocalDate) =
        transaction(database) {
            requests.forEach { (identifier, count) ->
                ResolvedTable.upsert(conflictIndex = ResolvedTable.uniqueIdentifierIdWithDate,
                    insertBody = {
                        it[ResolvedTable.identifierId] = findOrCreateIdentifierId(identifier)
                        it[ResolvedTable.date] = date
                        it[ResolvedTable.count] = count
                    },
                    updateBody = {
                        with(SqlExpressionBuilder) {
                            it[ResolvedTable.count] = ResolvedTable.count + count
                        }
                    }
                )
            }
        }

    private fun findOrCreateIdentifierId(identifier: Identifier): UUID =
        findIdentifier(identifier) ?: createIdentifier(identifier)

    private fun createIdentifier(identifier: Identifier): UUID  =
        identifier.toUUID().also { id ->
            IdentifierTable.insert {
                it[IdentifierTable.id] = id
                it[IdentifierTable.repository] = identifier.repository
                it[IdentifierTable.gav] = identifier.gav
            }
        }

    private fun findIdentifier(identifier: Identifier): UUID? =
        with(identifier.toUUID()) {
            IdentifierTable.select { IdentifierTable.id eq this@with }
                .firstOrNull()
                ?.let { it[IdentifierTable.id] }
        }

    override fun findResolvedRequestsByPhrase(repository: String, phrase: String, limit: Int): List<ResolvedEntry> =
        transaction(database) {
            val resolvedSum = ResolvedTable.count.sum()
            val whereCriteria =
                if (repository.isEmpty())
                    { IdentifierTable.gav like "%${phrase}%" }
                else
                    and({ IdentifierTable.repository eq repository }, { IdentifierTable.gav like "%${phrase}%" })

            IdentifierTable.leftJoin(ResolvedTable, { IdentifierTable.id }, { ResolvedTable.identifierId })
                .slice(IdentifierTable.gav, resolvedSum)
                .select(whereCriteria)
                .groupBy(ResolvedTable.id)
                .having { resolvedSum greater 0L }
                .orderBy(resolvedSum, DESC)
                .limit(limit)
                .filter { (it.getOrNull(resolvedSum) ?: 0) > 0 }
                .map { ResolvedEntry(it[IdentifierTable.gav], it[resolvedSum] ?: 0) }
        }

    override fun countUniqueResolvedRequests(): Long =
        transaction(database) {
            ResolvedTable.selectAll()
                .groupBy(ResolvedTable.identifierId)
                .count()
        }

    override fun countResolvedRequests(): Long =
        transaction(database) {
            with (ResolvedTable.count.sum()) {
                ResolvedTable.slice(this).selectAll().firstAndMap { it[this] }
            }
            ?: 0
        }

}