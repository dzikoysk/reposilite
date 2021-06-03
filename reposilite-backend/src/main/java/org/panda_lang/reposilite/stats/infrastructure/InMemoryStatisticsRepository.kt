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

import org.panda_lang.reposilite.stats.StatisticsRepository
import org.panda_lang.reposilite.stats.api.Record
import org.panda_lang.reposilite.stats.api.RecordIdentifier
import org.panda_lang.reposilite.stats.api.RecordType
import java.util.concurrent.ConcurrentHashMap

internal class InMemoryStatisticsRepository : StatisticsRepository {

    private val records: MutableMap<Int, Record> = ConcurrentHashMap()

    override fun createRecord(identifier: RecordIdentifier, initCount: Long) {
        records[records.size] = Record(
            id = records.size,
            type = identifier.type,
            identifier = identifier.identifier,
            count = initCount
        )
    }

    override fun incrementRecord(identifier: RecordIdentifier, count: Long) {
        findRecordByTypeAndIdentifier(identifier)
            ?.also { records[it.id] = it.copy(count = it.count + count) }
            ?: createRecord(identifier, count)
    }

    override fun findRecordByTypeAndIdentifier(identifier: RecordIdentifier): Record? =
        records.values.firstOrNull { it.type == identifier.type && it.identifier == identifier.identifier }

    override fun findRecordsByPhrase(type: RecordType, phrase: String): List<Record> =
        records.values
            .filter { it.type == type }
            .filter { it.identifier == phrase }

    override fun countRecords(): Long =
        records
            .map { it.value.count }
            .sum()

    override fun countUniqueRecords(): Long =
        records.size.toLong()

}