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
package com.reposilite.statistics

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.statistics.api.Record
import com.reposilite.statistics.api.RecordCountResponse
import com.reposilite.statistics.api.RecordIdentifier
import com.reposilite.statistics.api.RecordType
import com.reposilite.statistics.api.findRecordTypeByName
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.BAD_REQUEST
import panda.std.Result
import panda.std.asSuccess
import java.util.concurrent.ConcurrentHashMap

class StatisticsFacade internal constructor(
    private val journalist: Journalist,
    private val statisticsRepository: StatisticsRepository
) : Journalist {

    private val recordsBulk: ConcurrentHashMap<RecordIdentifier, Long> = ConcurrentHashMap()

    fun increaseRecord(type: RecordType, uri: String) =
        recordsBulk.merge(RecordIdentifier(type, uri), 1) { cached, value -> cached + value }

    suspend fun saveRecordsBulk() =
        recordsBulk.toMap().also {
            recordsBulk.clear() // read doesn't lock, so there is a possibility of dropping a few records between toMap and clear. Might be improved in the future
            statisticsRepository.incrementRecords(it)
            logger.debug("Statistics | Saved bulk with ${it.size} records")
        }

    suspend fun findRecordsByPhrase(type: String, phrase: String): Result<RecordCountResponse, ErrorResponse> =
        findRecordTypeByName(type)
            ?.let { findRecordsByPhrase(it, phrase).asSuccess() }
            ?: errorResponse(BAD_REQUEST, "Unknown record type $type}")

    suspend fun findRecordsByPhrase(type: RecordType, phrase: String): RecordCountResponse =
        statisticsRepository.findRecordsByPhrase(type, phrase)
            .let { RecordCountResponse(it.sumOf(Record::count), it) }

    suspend fun countUniqueRecords(): Long =
        statisticsRepository.countUniqueRecords()

    suspend fun countRecords(): Long =
        statisticsRepository.countRecords()

    override fun getLogger(): Logger =
        journalist.logger

}