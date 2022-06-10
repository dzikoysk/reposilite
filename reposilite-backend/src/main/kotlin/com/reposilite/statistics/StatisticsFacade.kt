/*
 * Copyright (c) 2022 dzikoysk
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
import com.reposilite.maven.api.Identifier
import com.reposilite.plugin.api.Facade
import com.reposilite.statistics.api.IncrementResolvedRequest
import com.reposilite.statistics.api.ResolvedCountResponse
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.BAD_REQUEST
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference
import java.util.concurrent.ConcurrentHashMap

const val MAX_PAGE_SIZE = 100

class StatisticsFacade internal constructor(
    private val journalist: Journalist,
    private val dateIntervalProvider: Reference<DateIntervalProvider>,
    private val statisticsRepository: StatisticsRepository
) : Journalist, Facade {

    private val resolvedRequestsBulk: ConcurrentHashMap<Identifier, Long> = ConcurrentHashMap()

    fun incrementResolvedRequest(incrementResolvedRequest: IncrementResolvedRequest) =
        resolvedRequestsBulk.merge(incrementResolvedRequest.identifier, incrementResolvedRequest.count) { cached, value -> cached + value }

    fun saveRecordsBulk() =
        resolvedRequestsBulk.toMap().also {
            resolvedRequestsBulk.clear() // read doesn't lock, so there is a possibility of dropping a few records between toMap and clear. Might be improved in the future
            statisticsRepository.incrementResolvedRequests(it, dateIntervalProvider.get().createDate())
            if (it.isNotEmpty()) logger.debug("Statistics | Saved bulk with ${it.size} records")
        }

    fun findResolvedRequestsByPhrase(repository: String = "", phrase: String, limit: Int = MAX_PAGE_SIZE): Result<ResolvedCountResponse, ErrorResponse> =
        limit.takeIf { it <= MAX_PAGE_SIZE }
            ?.let {
                statisticsRepository.findResolvedRequestsByPhrase(repository, phrase, limit).let {
                    ResolvedCountResponse(
                        it.sumOf { resolved -> resolved.count },
                        it
                    ).asSuccess()
                }
            }
            ?: errorResponse(BAD_REQUEST, "Requested too many records ($limit > $MAX_PAGE_SIZE)")

    fun countUniqueRecords(): Long =
        statisticsRepository.countUniqueResolvedRequests()

    fun countRecords(): Long =
        statisticsRepository.countResolvedRequests()

    override fun getLogger(): Logger =
        journalist.logger

}