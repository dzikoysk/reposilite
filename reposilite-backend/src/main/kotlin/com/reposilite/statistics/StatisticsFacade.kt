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
import com.reposilite.statistics.api.Identifier
import com.reposilite.statistics.api.IncrementResolvedRequest
import com.reposilite.statistics.api.ResolvedCountResponse
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

    private val resolvedRequestsBulk: ConcurrentHashMap<Identifier, Long> = ConcurrentHashMap()

    fun incrementResolvedRequest(incrementResolvedRequest: IncrementResolvedRequest) =
        resolvedRequestsBulk.merge(incrementResolvedRequest.identifier, incrementResolvedRequest.count) { cached, value -> cached + value }

    fun saveRecordsBulk() =
        resolvedRequestsBulk.toMap().also {
            resolvedRequestsBulk.clear() // read doesn't lock, so there is a possibility of dropping a few records between toMap and clear. Might be improved in the future
            statisticsRepository.incrementResolvedRequests(it)
            logger.debug("Statistics | Saved bulk with ${it.size} records")
        }

    fun findResolvedRequestsByPhrase(type: String, phrase: String, limit: Int = Int.MAX_VALUE): Result<ResolvedCountResponse, ErrorResponse> =
        findResolvedRequestsByPhrase(type)
            ?.let { findResolvedRequestsByPhrase(it, phrase, limit).asSuccess() }
            ?: errorResponse(BAD_REQUEST, "Unknown record type $type}")

    fun findResolvedRequestsByPhrase(identifier: Identifier: Identifier, phrase: String, limit: Int = Int.MAX_VALUE): ResolvedCountResponse =
        statisticsRepository.findRecordsByPhrase(type, phrase, limit)
            .let { ResolvedCountResponse(it.sumOf(Record::count), it) }

    fun countUniqueRecords(): Long =
        statisticsRepository.countUniqueResolvedRequests()

    fun countRecords(): Long =
        statisticsRepository.countResolvedRecords()

    override fun getLogger(): Logger =
        journalist.logger

}