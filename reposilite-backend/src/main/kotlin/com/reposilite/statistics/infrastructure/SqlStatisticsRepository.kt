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
import com.reposilite.statistics.api.Record
import com.reposilite.statistics.api.RecordIdentifier
import com.reposilite.statistics.api.RecordType
import kotlinx.coroutines.CoroutineDispatcher
import net.dzikoysk.exposed.upsert.upsert
import net.dzikoysk.exposed.upsert.withUnique
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op.Companion.build
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

const val MAX_IDENTIFIER_LENGTH = 1024

internal object StatisticsTable : Table("statistics") {
    val type = varchar("type", 16).index("idx_type")
    val identifier_id = uuid("identifier_id").index("idx_identifier_id")
    val identifier = varchar("identifier", MAX_IDENTIFIER_LENGTH)
    val count = long("count")

    val statisticsTypeWithIdentifierKey = withUnique("uq_type_identifier_id", type, identifier_id)
}

internal class SqlStatisticsRepository(private val dispatcher: CoroutineDispatcher, private val database: Database) : StatisticsRepository {

    init {
        transaction(database) {
            SchemaUtils.create(StatisticsTable)
        }
    }

    override suspend fun incrementRecord(record: RecordIdentifier, count: Long) =
        newSuspendedTransaction(dispatcher, database) {
            rawIncrementRecord(record, count)
        }

    override suspend fun incrementRecords(bulk: Map<RecordIdentifier, Long>) =
        newSuspendedTransaction(dispatcher, database) {
            bulk.forEach { rawIncrementRecord(it.key, it.value) }
        }

    private fun rawIncrementRecord(record: RecordIdentifier, count: Long) {
        if (record.identifier.length > MAX_IDENTIFIER_LENGTH) {
            throw UnsupportedOperationException("Identifier '${record.identifier}' exceeds allowed length")
        }

        StatisticsTable.upsert(conflictIndex = StatisticsTable.statisticsTypeWithIdentifierKey,
            insertBody = {
                it[this.type] = record.type.name
                it[this.identifier_id] = UUID.nameUUIDFromBytes(record.identifier.toByteArray())
                it[this.identifier] = record.identifier
                it[this.count] = count
            },
            updateBody = {
                with(SqlExpressionBuilder) {
                    it[StatisticsTable.count] = StatisticsTable.count + count
                }
            }
        )
    }

    private fun toRecord(row: ResultRow) =
        Record(
            RecordType.valueOf(row[StatisticsTable.type].uppercase()),
            row[StatisticsTable.identifier],
            row[StatisticsTable.count]
        )

    override suspend fun findRecordByTypeAndIdentifier(record: RecordIdentifier): Record? =
        newSuspendedTransaction(dispatcher, database) {
            StatisticsTable.select { build { StatisticsTable.type eq record.type.name }.and { StatisticsTable.identifier eq record.identifier } }
                .firstAndMap { toRecord(it) }
        }

    override suspend fun findRecordsByPhrase(type: RecordType, phrase: String): List<Record> =
        newSuspendedTransaction(dispatcher, database) {
            StatisticsTable.select { build { StatisticsTable.type eq type.name }.and { StatisticsTable.identifier like "%${phrase}%" }}
                .map { toRecord(it) }
        }

    override suspend fun countRecords(): Long =
        newSuspendedTransaction(dispatcher, database) {
            with (StatisticsTable.count.sum()) {
                StatisticsTable.slice(this).selectAll().firstAndMap { it[this] }
            }
            ?: 0
        }

    override suspend fun countUniqueRecords(): Long =
        newSuspendedTransaction(dispatcher, database) {
            StatisticsTable.selectAll().count()
        }

}