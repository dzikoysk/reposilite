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

package com.reposilite.statistics.infrastructure

import com.reposilite.DatabaseMigrations.MIGRATION_001
import com.reposilite.DatabaseMigrations.MIGRATION_002
import com.reposilite.DatabaseMigrations.MIGRATION_003
import com.reposilite.journalist.Journalist
import com.reposilite.maven.api.GAV_MAX_LENGTH
import com.reposilite.maven.api.Identifier
import com.reposilite.maven.api.REPOSITORY_NAME_MAX_LENGTH
import com.reposilite.shared.extensions.executeQuery
import com.reposilite.statistics.StatisticsRepository
import com.reposilite.statistics.api.ResolvedEntry
import com.reposilite.statistics.toUTCMillis
import net.dzikoysk.exposed.upsert.upsert
import net.dzikoysk.exposed.upsert.withUnique
import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.statements.BatchUpdateStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import panda.std.firstAndMap
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Suppress("RemoveRedundantQualifierName")
internal class SqlStatisticsRepository(
    private val database: Database,
    private val journalist: Journalist,
    private val runMigrations: Array<String>
) : StatisticsRepository {

    object IdentifierTable : Table("statistics_identifier") {
        val id = uuid("identifier_id")
        val repository = varchar("repository", REPOSITORY_NAME_MAX_LENGTH).index("idx_statistics_identifier_repository")
        val gav = varchar("gav", GAV_MAX_LENGTH)

        override val primaryKey = PrimaryKey(id)
    }

    object ResolvedTable : IntIdTable("statistics_resolved_identifier") {
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
        runFixes()
    }

    private fun runFixes() {
        // 001 Migration: Change `repository` identifier size from 32 to 64.
        if (MIGRATION_001 in runMigrations) {
            transaction(database) {
                val connection = TransactionManager.current().connection

                when (val dialect = db.vendor.lowercase()) {
                    "postgresql" -> {
                        connection.executeQuery("ALTER TABLE statistics_identifier ALTER COLUMN repository TYPE VARCHAR($REPOSITORY_NAME_MAX_LENGTH);")
                    }
                    "mysql", "mariadb", "h2" -> {
                        connection.executeQuery("ALTER TABLE statistics_identifier MODIFY repository VARCHAR($REPOSITORY_NAME_MAX_LENGTH);")
                    }
                    "sqlite" -> {
                        connection.executeQuery("PRAGMA writable_schema = 1;")
                        connection.executeQuery("UPDATE SQLITE_MASTER SET SQL = replace(sql, 'repository VARCHAR(32)', 'repository VARCHAR($REPOSITORY_NAME_MAX_LENGTH)') WHERE name='statistics_identifier' AND type='table';")
                        connection.executeQuery("PRAGMA writable_schema = 0;")
                    }
                    else -> throw UnsupportedOperationException("Unsupported SQL dialect $dialect")
                }
            }
        }

        // 002 Fix: Remove `.module` entries from records
        if (MIGRATION_002 in runMigrations) {
            transaction(database) {
                ResolvedTable.leftJoin(IdentifierTable, { ResolvedTable.identifierId }, { IdentifierTable.id })
                    .selectAll()
                    .where { IdentifierTable.gav like "%.module" }
                    .map { it[ResolvedTable.identifierId] }
                    .takeIf { it.isNotEmpty() }
                    ?.also { journalist.logger.info("SqlStatisticsRepository | ${it.size} '%.module' entries will be removed from database") }
                    ?.forEach { id -> ResolvedTable.deleteWhere { ResolvedTable.identifierId eq id } }
            }
        }

        // 003 Fix: Convert timestamp dates in SQLite to ISO format
        if (MIGRATION_003 in runMigrations && database.vendor.lowercase() == "sqlite") {
            transaction(database) {
                @Language("sqlite")
                val query = "SELECT id, date FROM statistics_resolved_identifier WHERE date NOT LIKE '%-%';".trimIndent()

                val statement = TransactionManager.current().connection.prepareStatement(query, false)
                val result = statement.executeQuery()
                val resolvedRequestIdToTimestamp = mutableMapOf<Int, String>()

                while (result.next()) {
                    val id = result.getInt(ResolvedTable.id.name)
                    val timestamp = result.getString(ResolvedTable.date.name)
                    resolvedRequestIdToTimestamp[id] = timestamp
                }

                BatchUpdateStatement(ResolvedTable).apply {
                    resolvedRequestIdToTimestamp
                        .mapValues { it.value.toLongOrNull() }
                        .filterValues { it != null }
                        .mapValues { it.value!! }
                        .forEach {
                            addBatch(EntityID(it.key, ResolvedTable))
                            this[ResolvedTable.date] = Instant.ofEpochMilli(it.value).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                    execute(TransactionManager.current())
                }
            }
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

    private fun createIdentifier(identifier: Identifier): UUID =
        identifier.toUUID().also { id ->
            IdentifierTable.insert {
                it[IdentifierTable.id] = id
                it[IdentifierTable.repository] = identifier.repository
                it[IdentifierTable.gav] = identifier.gav
            }
        }

    private fun findIdentifier(identifier: Identifier): UUID? =
        with(identifier.toUUID()) {
            IdentifierTable
                .selectAll()
                .where { IdentifierTable.id eq this@with }
                .firstOrNull()
                ?.let { it[IdentifierTable.id] }
        }

    override fun findResolvedRequestsByPhrase(repository: String, phrase: String, limit: Int): List<ResolvedEntry> =
        transaction(database) {
            val resolvedSum = ResolvedTable.count.sum()
            val whereCriteria =
                if (repository.isEmpty())
                    IdentifierTable.gav like "%$phrase%"
                else
                    AndOp(listOf(Op.build { IdentifierTable.repository eq repository }, Op.build { IdentifierTable.gav like "%$phrase%" }))

            IdentifierTable.leftJoin(ResolvedTable, { IdentifierTable.id }, { ResolvedTable.identifierId })
                .select(IdentifierTable.gav, resolvedSum)
                .where(whereCriteria)
                .groupBy(IdentifierTable.id, IdentifierTable.gav)
                .having { resolvedSum greater 0L }
                .orderBy(resolvedSum, DESC)
                .limit(limit)
                .filter { (it.getOrNull(resolvedSum) ?: 0) > 0 }
                .map { ResolvedEntry(it[IdentifierTable.gav], it[resolvedSum] ?: 0) }
        }

    override fun getAllResolvedRequestsPerRepositoryAsTimeSeries(): Map<String, Map<LocalDate, Long>> =
        transaction(database) {
            val start = LocalDate.now().minusYears(1).withDayOfMonth(1)

            ResolvedTable.leftJoin(IdentifierTable, { ResolvedTable.identifierId }, { IdentifierTable.id })
                .select(IdentifierTable.repository, ResolvedTable.date, ResolvedTable.count.sum())
                .where { ResolvedTable.date greaterEq start }
                .groupBy(IdentifierTable.repository, ResolvedTable.date)
                .asSequence()
                .map { Triple(it[IdentifierTable.repository], it[ResolvedTable.date], it[ResolvedTable.count.sum()]) }
                .groupBy(
                    keySelector = { (repository, _, _) -> repository },
                    valueTransform = { (_, date, count) -> date to (count ?: 0) }
                )
                .mapValues { (_, records) -> records.toMap() }
        }

    override fun countUniqueResolvedRequests(): Long =
        transaction(database) {
            ResolvedTable.selectAll()
                .groupBy(ResolvedTable.id, ResolvedTable.identifierId)
                .count()
        }

    override fun countResolvedRequests(): Long =
        transaction(database) {
            with (ResolvedTable.count.sum()) {
                ResolvedTable.select(this).firstAndMap { it[this] } ?: 0
            }
        }

}
