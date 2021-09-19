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

import com.reposilite.statistics.StatisticsRepository
import com.reposilite.statistics.api.Record
import com.reposilite.statistics.api.RecordIdentifier
import com.reposilite.statistics.api.RecordType
import java.util.concurrent.ConcurrentHashMap

internal class InMemoryStatisticsRepository : StatisticsRepository {

    private val records: MutableMap<RecordIdentifier, Record> = ConcurrentHashMap()

    override suspend fun incrementRecord(record: RecordIdentifier, count: Long) {
        findRecordByTypeAndIdentifier(record)
            ?.also { records[record] = it.copy(count = it.count + count) }
            ?: createRecord(record, count)
    }

    override suspend fun incrementRecords(bulk: Map<RecordIdentifier, Long>) {
        bulk.forEach { incrementRecord(it.key, it.value) }
    }

    private fun createRecord(identifier: RecordIdentifier, initCount: Long) {
        records[identifier] = Record(
            type = identifier.type,
            identifier = identifier.identifier,
            count = initCount
        )
    }

    override suspend fun findRecordByTypeAndIdentifier(record: RecordIdentifier): Record? =
        records.values.firstOrNull { it.type == record.type && it.identifier == record.identifier }

    override suspend fun findRecordsByPhrase(type: RecordType, phrase: String, limit: Int): List<Record> =
        records.values.asSequence()
            .filter { it.type == type }
            .filter { it.identifier.contains(phrase) }
            .take(limit)
            .sortedBy { it.count }
            .toList()

    override suspend fun countRecords(): Long =
        records
            .map { it.value.count }
            .sum()

    override suspend fun countUniqueRecords(): Long =
        records.size.toLong()

}