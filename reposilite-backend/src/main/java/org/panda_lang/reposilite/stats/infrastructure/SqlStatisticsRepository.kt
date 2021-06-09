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

package org.panda_lang.reposilite.stats.infrastructure

import org.jetbrains.exposed.sql.Op.Companion.build
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.panda_lang.reposilite.shared.sql.firstAndMap
import org.panda_lang.reposilite.shared.sql.transactionUnit
import org.panda_lang.reposilite.stats.StatisticsRepository
import org.panda_lang.reposilite.stats.api.Record
import org.panda_lang.reposilite.stats.api.RecordIdentifier
import org.panda_lang.reposilite.stats.api.RecordType

internal class SqlStatisticsRepository : StatisticsRepository {

    init {
        transaction {
            SchemaUtils.create(StatisticsTable)
        }
    }

    override fun createRecord(identifier: RecordIdentifier, initCount: Long) =
        transactionUnit {
            StatisticsTable.insert {
                it[this.type] = identifier.type.name
                it[this.identifier] = identifier.identifier
                it[this.count] = initCount
            }
        }

    override fun incrementRecord(identifier: RecordIdentifier, count: Long) =
        transactionUnit {
            val id = StatisticsTable
                .select { build { StatisticsTable.type eq identifier.type.name }.and { StatisticsTable.identifier eq identifier.identifier } }
                .firstAndMap { it[StatisticsTable.id].value }

            if (id == null) {
                StatisticsTable.insert {
                    it[this.type] = identifier.type.name
                    it[this.identifier] = identifier.identifier
                    it[this.count] = count
                }
            }
            else {
                StatisticsTable.update({ StatisticsTable.id eq id }) {
                    with(SqlExpressionBuilder) {
                        it[StatisticsTable.count] = StatisticsTable.count + count
                    }
                }
            }

        }

    private fun toRecord(row: ResultRow) =
        Record(
            row[StatisticsTable.id].value,
            RecordType.valueOf(row[StatisticsTable.type].toUpperCase()),
            row[StatisticsTable.identifier],
            row[StatisticsTable.count]
        )

    override fun findRecordByTypeAndIdentifier(identifier: RecordIdentifier): Record? =
        transaction {
            StatisticsTable.select { build { StatisticsTable.type eq identifier.type.name }.and { StatisticsTable.identifier eq identifier.identifier } }
                .firstAndMap { toRecord(it) }
        }

    override fun findRecordsByPhrase(type: RecordType, phrase: String): List<Record> =
        transaction {
            StatisticsTable.select { build { StatisticsTable.type eq type.name }.and { StatisticsTable.identifier like "%${phrase}%" }}
                .map { toRecord(it) }
        }

    override fun countRecords(): Long =
        transaction {
            StatisticsTable.count.sum().let { countSum ->
                StatisticsTable.slice(countSum).selectAll()
                    .first()
                    .let { it[countSum] }
                    ?: 0
            }
        }

    override fun countUniqueRecords(): Long =
        transaction {
            StatisticsTable.selectAll().count()
        }

}