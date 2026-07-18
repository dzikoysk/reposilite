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
package com.reposilite.statistics

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.api.Identifier
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.statistics.api.AllResolvedResponse
import com.reposilite.statistics.api.IncrementResolvedRequest
import com.reposilite.statistics.api.IntervalRecord
import com.reposilite.statistics.api.RepositoryStatistics
import com.reposilite.statistics.api.ResolvedCountResponse
import com.reposilite.statistics.api.ResolvedEntriesPage
import com.reposilite.statistics.api.ResolvedEntriesResponse
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission.READ
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference
import java.util.concurrent.ConcurrentHashMap

const val MAX_PAGE_SIZE = 100

class StatisticsFacade internal constructor(
    private val journalist: Journalist,
    private val statisticsEnabled: Reference<Boolean>,
    private val dateIntervalProvider: Reference<DateIntervalProvider>,
    private val statisticsRepository: StatisticsRepository,
    private val accessTokenFacade: AccessTokenFacade
) : Journalist, Facade {

    private val resolvedRequestsBulk: ConcurrentHashMap<Identifier, Long> = ConcurrentHashMap()

    fun incrementResolvedRequest(incrementResolvedRequest: IncrementResolvedRequest) {
        when {
            statisticsEnabled.get() -> resolvedRequestsBulk.merge(incrementResolvedRequest.identifier, incrementResolvedRequest.count) { cached, value -> cached + value }
            else -> logger.debug("Statistics | Cannot increment ${incrementResolvedRequest.identifier}, because statistics are disabled")
        }
    }

    fun saveRecordsBulk() =
        resolvedRequestsBulk.toMap()
            .takeIf { it.isNotEmpty() }
            ?.also {
                resolvedRequestsBulk.clear() // read doesn't lock, so there is a possibility of dropping a few records between toMap and clear. Might be improved in the future
                statisticsRepository.incrementResolvedRequests(it, dateIntervalProvider.get().createDate())
                logger.debug("Statistics | Saved bulk with ${it.size} records")
            }

    fun findResolvedRequestsByPhrase(
        repository: String = "",
        phrase: String,
        limit: Int = MAX_PAGE_SIZE,
        accessToken: AccessTokenIdentifier? = null
    ): Result<ResolvedCountResponse, ErrorResponse> =
        limit.takeIf { it in 1..MAX_PAGE_SIZE }
            ?.let {
                val accessibleGavPrefixes = accessToken?.let { getAccessibleGavPrefixes(it, repository) }
                statisticsRepository.findResolvedRequestsByPhrase(repository, phrase, limit, accessibleGavPrefixes).let {
                    ResolvedCountResponse(
                        sum = it.sumOf { resolved -> resolved.count },
                        requests = it
                    ).asSuccess()
                }
            }
            ?: badRequestError("Requested invalid page size ($limit, expected 1..$MAX_PAGE_SIZE)")

    private fun getAccessibleGavPrefixes(accessToken: AccessTokenIdentifier, repository: String): Set<String>? =
        if (accessTokenFacade.hasPermission(accessToken, MANAGER)) {
            null
        } else {
            accessTokenFacade.getRoutes(accessToken)
                .asSequence()
                .filter { it.permission == READ }
                .mapNotNull { it.toGavPrefix(repository) }
                .toSet()
        }

    private fun Route.toGavPrefix(repository: String): String? {
        val repositoryRoot = "/$repository"
        return when {
            repositoryRoot.startsWith(path, ignoreCase = true) -> ""
            path.startsWith("$repositoryRoot/", ignoreCase = true) -> path.substring(repositoryRoot.length + 1)
            else -> null
        }
    }

    fun findResolvedEntries(repository: String?, phrase: String = "", limit: Int = MAX_PAGE_SIZE, offset: Long = 0): Result<ResolvedEntriesResponse, ErrorResponse> =
        when {
            limit !in 1..MAX_PAGE_SIZE ->
                badRequestError("Requested invalid page size ($limit, expected 1..$MAX_PAGE_SIZE)")
            offset < 0 ->
                badRequestError("Requested invalid offset ($offset, expected >= 0)")
            else -> {
                val records = statisticsRepository.findResolvedEntries(repository, phrase, limit + 1, offset)
                val entries = records.take(limit)
                ResolvedEntriesResponse(
                    page = ResolvedEntriesPage(
                        limit = limit,
                        offset = offset,
                        hasMore = records.size > limit,
                        nextOffset = if (records.size > limit) offset + limit else null
                    ),
                    entries = entries
                ).asSuccess()
            }
        }

    fun getAllResolvedStatistics(): Result<AllResolvedResponse, ErrorResponse> =
        when {
            statisticsEnabled.get() ->
                AllResolvedResponse(
                    interval = dateIntervalProvider.get().interval,
                    repositories =
                        statisticsRepository.getAllResolvedRequestsPerRepositoryAsTimeSeries()
                            .mapValues { (_, records) ->
                                val timeSeries = dateIntervalProvider
                                    .map { it.createTimeSeries() }
                                    .associateWith { 0L }

                                (timeSeries + records)
                                    .asSequence()
                                    .map { (date, count) -> IntervalRecord(date.toUTCMillis(), count) }
                                    .sortedBy { it.date }
                                    .toList()
                            }
                            .asSequence()
                            .map { (repository, records) -> RepositoryStatistics(repository, records) }
                            .sortedWith(compareBy({ repository -> -repository.data.sumOf { it.count } }, { it.name }))
                            .toList()
                )
            else ->
                AllResolvedResponse(
                    statisticsEnabled = false,
                    interval = dateIntervalProvider.get().interval
                )
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
