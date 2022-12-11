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
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.statistics.api.AllResolvedResponse
import com.reposilite.statistics.api.IncrementResolvedRequest
import com.reposilite.statistics.api.ResolvedCountResponse
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference
import java.util.concurrent.ConcurrentHashMap

const val MAX_PAGE_SIZE = 100

class StatisticsFacade internal constructor(
    private val journalist: Journalist,
    private val statisticsEnabled: Reference<Boolean>,
    private val dateIntervalProvider: Reference<DateIntervalProvider>,
    private val statisticsRepository: StatisticsRepository
) : Journalist, Facade {

    private val resolvedRequestsBulk: ConcurrentHashMap<Identifier, Long> = ConcurrentHashMap()

    fun incrementResolvedRequest(incrementResolvedRequest: IncrementResolvedRequest) {
        when {
            statisticsEnabled.get() -> resolvedRequestsBulk.merge(incrementResolvedRequest.identifier, incrementResolvedRequest.count) { cached, value -> cached + value }
            else -> logger.debug("Statistics | Cannot increment ${incrementResolvedRequest.identifier}, because statistics are disabled")
        }
    }

    fun saveRecordsBulk() =
        resolvedRequestsBulk.toMap().also {
            resolvedRequestsBulk.clear() // read doesn't lock, so there is a possibility of dropping a few records between toMap and clear. Might be improved in the future
            statisticsRepository.incrementResolvedRequests(it, dateIntervalProvider.get().createDate())
            logger.debug("Statistics | Saved bulk with ${it.size} records")
        }

    fun findResolvedRequestsByPhrase(repository: String = "", phrase: String, limit: Int = MAX_PAGE_SIZE): Result<ResolvedCountResponse, ErrorResponse> =
        limit.takeIf { it <= MAX_PAGE_SIZE }
            ?.let {
                statisticsRepository.findResolvedRequestsByPhrase(repository, phrase, limit).let {
                    ResolvedCountResponse(
                        sum = it.sumOf { resolved -> resolved.count },
                        requests = it
                    ).asSuccess()
                }
            }
            ?: badRequestError("Requested too many records ($limit > $MAX_PAGE_SIZE)")

    fun getAllResolvedStatistics(): Result<AllResolvedResponse, ErrorResponse> =
        when {
            statisticsEnabled.get() -> AllResolvedResponse(repositories = statisticsRepository.getAllResolvedRequestsPerRepositoryAsTimeseries())
            else -> AllResolvedResponse(statisticsEnabled = false)
        }.asSuccess()

    fun countUniqueRecords(): Long =
        statisticsRepository.countUniqueResolvedRequests()

    fun countRecords(): Long =
        statisticsRepository.countResolvedRequests()

    fun statisticsEnabled(): Reference<Boolean> =
        statisticsEnabled

    override fun getLogger(): Logger =
        journalist.logger

}
