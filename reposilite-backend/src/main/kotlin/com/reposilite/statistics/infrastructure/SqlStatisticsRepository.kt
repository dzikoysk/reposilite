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
import com.reposilite.statistics.api.ResolvedStatisticsEntry
import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
import org.jetbrains.exposed.v1.core.SortOrder.ASC
import org.jetbrains.exposed.v1.core.SortOrder.DESC
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import panda.std.firstAndMap
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

private val MYSQL_FAMILY_VENDORS = setOf("mysql", "mariadb", "h2")

@Suppress("RemoveRedundantQualifierName")
internal class SqlStatisticsRepository(
    private val database: Database,
    private val journalist: Journalist,
    private val runMigrations: Array<String>
) : StatisticsRepository {

    object IdentifierTable : Table("statistics_identifier") {
        val id = javaUUID("identifier_id")
        val repository = varchar("repository", REPOSITORY_NAME_MAX_LENGTH).index("idx_statistics_identifier_repository")
        val gav = varchar("gav", GAV_MAX_LENGTH)

        override val primaryKey = PrimaryKey(id)
    }

    object ResolvedTable : IntIdTable("statistics_resolved_identifier") {
        val identifierId = reference("identifier_id", IdentifierTable.id, onDelete = CASCADE, onUpdate = CASCADE)
        val date = date("date")
        val count = long("count")

        init {
            uniqueIndex("uq_statistics_resolved_identifier_identifier_id_date", identifierId, date)
        }
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
                val query = "SELECT id, identifier_id, date, count FROM statistics_resolved_identifier WHERE date NOT LIKE '%-%';".trimIndent()

                val statement = TransactionManager.current().connection.prepareStatement(query, false)
                val result = statement.executeQuery().result
                val resolvedRequestIdToTimestamp = mutableMapOf<Int, Triple<UUID, LocalDate, Long>>()

                while (result.next()) {
                    val id = result.getInt(ResolvedTable.id.name)
                    val timestamp = result.getString(ResolvedTable.date.name)
                    val count = result.getLong(ResolvedTable.count.name)

                    val identifierIdBytes = ByteBuffer.wrap(result.getBytes(ResolvedTable.identifierId.name))
                    val identifierId = UUID(identifierIdBytes.getLong(), identifierIdBytes.getLong())

                    resolvedRequestIdToTimestamp[id] = Triple(
                        identifierId,
                        Instant.ofEpochMilli(timestamp.toLong()).atZone(ZoneId.systemDefault()).toLocalDate(),
                        count
                    )
                }

                var updated = 0
                var merged = 0

                resolvedRequestIdToTimestamp.forEach { (id, value) ->
                    val (identifier, date, count) = value

                    try {
                        ResolvedTable.update({ ResolvedTable.id eq id }) {
                            it[ResolvedTable.date] = date
                        }
                        updated++
                    } catch (sqliteException: Exception) {
                        ResolvedTable.update({ (ResolvedTable.identifierId eq identifier) and (ResolvedTable.date eq date)}) {
                            it[ResolvedTable.count] = ResolvedTable.count + count
                        }
                        merged++
                    }
                }

                journalist.logger.info("SqlStatisticsRepository | $updated records updated, $merged records merged")
            }
        }
    }

    override fun incrementResolvedRequests(requests: Map<Identifier, Long>, date: LocalDate) =
        transaction(database) {
            // MySQL/MariaDB/H2-in-MySQL-mode reject explicit conflict keys in ON DUPLICATE KEY UPDATE;
            // they infer the conflict from any unique constraint on the table (the uniqueIndex declared on
            // (identifierId, date) in ResolvedTable). PostgreSQL/SQLite require explicit conflict columns.
            val conflictKeys: Array<Column<*>> =
                if (database.vendor.lowercase() in MYSQL_FAMILY_VENDORS) emptyArray()
                else arrayOf(ResolvedTable.identifierId, ResolvedTable.date)

            requests.forEach { (identifier, count) ->
                ResolvedTable.upsert(
                    *conflictKeys,
                    onUpdate = {
                        it[ResolvedTable.count] = ResolvedTable.count + count
                    }
                ) {
                    it[ResolvedTable.identifierId] = findOrCreateIdentifierId(identifier)
                    it[ResolvedTable.date] = date
                    it[ResolvedTable.count] = count
                }
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

    override fun findResolvedRequestsByPhrase(
        repository: String,
        phrase: String,
        limit: Int,
        accessibleGavPrefixes: Set<String>?
    ): List<ResolvedEntry> =
        transaction(database) {
            if (accessibleGavPrefixes?.isEmpty() == true) {
                return@transaction emptyList()
            }

            val resolvedSum = ResolvedTable.count.sum()
            val restrictedPrefixes = accessibleGavPrefixes?.takeUnless { "" in it }
            val whereCriteria = listOfNotNull(
                repository.takeIf(String::isNotEmpty)?.let { IdentifierTable.repository eq it },
                phrase.takeIf(String::isNotEmpty)?.let { IdentifierTable.gav.lowerCase() like it.toContainsPattern() },
                restrictedPrefixes?.let { prefixes ->
                    OrOp(prefixes.map { IdentifierTable.gav.lowerCase() like (LikePattern.ofLiteral(it.lowercase()) + "%") })
                }
            ).let { if (it.isEmpty()) Op.TRUE else AndOp(it) }

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

    override fun findResolvedEntries(
        repository: String?,
        phrase: String,
        from: LocalDate,
        limit: Int,
        offset: Long
    ): List<ResolvedStatisticsEntry> =
        transaction(database) {
            val resolvedSum = ResolvedTable.count.sum()
            val criteria = listOfNotNull<Op<Boolean>>(
                ResolvedTable.date greaterEq from,
                phrase.takeIf(String::isNotEmpty)?.let { IdentifierTable.gav.lowerCase() like it.toContainsPattern() },
                repository?.let { IdentifierTable.repository eq it }
            )

            IdentifierTable.leftJoin(ResolvedTable, { IdentifierTable.id }, { ResolvedTable.identifierId })
                .select(IdentifierTable.repository, IdentifierTable.gav, resolvedSum)
                .where(AndOp(criteria))
                .groupBy(IdentifierTable.id, IdentifierTable.repository, IdentifierTable.gav)
                .having { resolvedSum greater 0L }
                .orderBy(
                    resolvedSum to DESC,
                    IdentifierTable.repository to ASC,
                    IdentifierTable.gav to ASC
                )
                .limit(limit)
                .offset(offset)
                .filter { (it.getOrNull(resolvedSum) ?: 0) > 0 }
                .map { ResolvedStatisticsEntry(it[IdentifierTable.repository], it[IdentifierTable.gav], it[resolvedSum] ?: 0) }
        }

    override fun getAllResolvedRequestsPerRepositoryAsTimeSeries(from: LocalDate): Map<String, Map<LocalDate, Long>> =
        transaction(database) {
            ResolvedTable.leftJoin(IdentifierTable, { ResolvedTable.identifierId }, { IdentifierTable.id })
                .select(IdentifierTable.repository, ResolvedTable.date, ResolvedTable.count.sum())
                .where { ResolvedTable.date greaterEq from }
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
            val uniqueIdentifiers = ResolvedTable.identifierId.countDistinct()
            ResolvedTable.select(uniqueIdentifiers).first()[uniqueIdentifiers]
        }

    override fun countResolvedRequests(): Long =
        transaction(database) {
            with (ResolvedTable.count.sum()) {
                ResolvedTable.select(this).firstAndMap { it[this] } ?: 0
            }
        }

}

private fun String.toContainsPattern(): LikePattern =
    LikePattern.ofLiteral(lowercase()).let { it.copy(pattern = "%${it.pattern}%") }
