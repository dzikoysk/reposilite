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

import org.jetbrains.exposed.sql.Op
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
import org.panda_lang.reposilite.stats.Record
import org.panda_lang.reposilite.stats.RecordType
import org.panda_lang.reposilite.stats.StatisticsRepository

class SqlStatisticsRepository : StatisticsRepository {

    init {
        transaction {
            SchemaUtils.create(StatisticsTable)
        }
    }

    override fun incrementRecords(bulk: Map<Pair<RecordType, String>, Long>) {
        transaction {
            bulk.forEach { (type, identifier), count ->
                val id: Int? = StatisticsTable
                    .select { Op.build { StatisticsTable.type eq type }.and { StatisticsTable.identifier eq identifier } }
                    .map { it[StatisticsTable.id].value }
                    .firstOrNull()

                if (id == null) {
                    StatisticsTable.insert {
                        it[StatisticsTable.type] = type
                        it[StatisticsTable.identifier] = identifier
                        it[StatisticsTable.count] = count
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
        }
    }

    override fun findRecordsByPhrase(type: RecordType, phrase: String): List<Record> = transaction {
        StatisticsTable
            .select { Op.build { StatisticsTable.type eq type }.and { StatisticsTable.identifier like "%${phrase}%" }}
            .map { toRecord(it) }
    }

    private fun toRecord(row: ResultRow) =
        Record(
            row[StatisticsTable.id].value,
            row[StatisticsTable.type],
            row[StatisticsTable.identifier],
            row[StatisticsTable.count]
        )

    override fun countUniqueRecords(): Long = transaction {
        StatisticsTable.selectAll().count()
    }

    override fun countRecords(): Long = transaction {
        val countSum = StatisticsTable.count.sum()

        StatisticsTable
            .slice(countSum)
            .selectAll()
            .first()
            .let { it[countSum] }
            ?: 0
    }

}