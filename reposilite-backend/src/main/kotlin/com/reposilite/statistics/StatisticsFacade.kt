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
import com.reposilite.shared.DateRange
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.shared.toErrorResult
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
import io.javalin.http.HttpStatus.FORBIDDEN
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

const val MAX_PAGE_SIZE = 100
// Statistics were introduced with Reposilite 3.0.0.
internal val STATISTICS_EARLIEST_DATE: LocalDate = LocalDate.of(2022, 8, 1)

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
    ): Result<ResolvedCountResponse, ErrorResponse> {
        if (limit !in 1..MAX_PAGE_SIZE) {
            return badRequestError("Requested invalid page size ($limit, expected 1..$MAX_PAGE_SIZE)")
        }

        val accessibleGavPrefixes: Result<Set<String>?, ErrorResponse> =
            accessToken?.let { getAccessibleGavPrefixes(it, repository, phrase) } ?: Result.ok(null)

        return accessibleGavPrefixes.map { prefixes ->
            val requests = statisticsRepository.findResolvedRequestsByPhrase(repository, phrase, limit, prefixes)
            ResolvedCountResponse(
                sum = requests.sumOf { it.count },
                requests = requests
            )
        }
    }

    private fun getAccessibleGavPrefixes(
        accessToken: AccessTokenIdentifier,
        repository: String,
        phrase: String
    ): Result<Set<String>?, ErrorResponse> {
        if (accessTokenFacade.hasPermission(accessToken, MANAGER)) {
            return Result.ok(null)
        }

        val routes = accessTokenFacade.getRoutes(accessToken)

        if (routes.none { it.hasPermissionTo(resolvedPath(repository, phrase), READ) }) {
            return FORBIDDEN.toErrorResult("This token is not authorized to access this path")
        }

        val prefixes: Set<String>? = routes.asSequence()
            .filter { it.permission == READ }
            .mapNotNull { it.toGavPrefix(repository) }
            .toSet()

        return Result.ok(prefixes)
    }

    private fun Route.toGavPrefix(repository: String): String? {
        val repositoryRoot = "/$repository"
        return when {
            repositoryRoot.startsWith(path, ignoreCase = true) -> ""
            path.startsWith("$repositoryRoot/", ignoreCase = true) -> path.substring(repositoryRoot.length + 1)
            else -> null
        }
    }

    fun findResolvedEntries(
        repository: String?,
        phrase: String = "",
        limit: Int = MAX_PAGE_SIZE,
        offset: Long = 0,
        dateRange: DateRange? = null
    ): Result<ResolvedEntriesResponse, ErrorResponse> {
        val requestedRange = dateRange?.coerceToStatisticsBounds()

        return when {
            limit !in 1..MAX_PAGE_SIZE ->
                badRequestError("Requested invalid page size ($limit, expected 1..$MAX_PAGE_SIZE)")
            offset < 0 ->
                badRequestError("Requested invalid offset ($offset, expected >= 0)")
            requestedRange?.isEmpty() == true ->
                badRequestError("Requested invalid statistics date range (${requestedRange.from} must not be after ${requestedRange.to})")
            else -> {
                val range = requestedRange ?: DateRange(from = dateIntervalProvider.get().createTimeSeries().min())
                val records = statisticsRepository.findResolvedEntries(repository, phrase, range, limit + 1, offset)
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
    }

    fun getAllResolvedStatistics(dateRange: DateRange? = null): Result<AllResolvedResponse, ErrorResponse> {
        val intervalProvider = dateIntervalProvider.get()
        val requestedRange = dateRange?.coerceToStatisticsBounds()

        if (requestedRange?.isEmpty() == true) {
            return badRequestError("Requested invalid statistics date range (${requestedRange.from} must not be after ${requestedRange.to})")
        }

        return when {
            statisticsEnabled.get() -> {
                val defaultTimeSeries = intervalProvider.createTimeSeries()
                val range = requestedRange ?: DateRange(from = defaultTimeSeries.min())
                val recordsByRepository = statisticsRepository.getAllResolvedRequestsPerRepositoryAsTimeSeries(range)
                val timeSeries = when {
                    dateRange == null -> defaultTimeSeries
                    recordsByRepository.isEmpty() -> emptyList()
                    else -> intervalProvider.createTimeSeries(
                        requireNotNull(range.from).coerceAtLeast(STATISTICS_EARLIEST_DATE),
                        requireNotNull(range.to)
                    )
                }.associateWith { 0L }

                AllResolvedResponse(
                    interval = intervalProvider.interval,
                    repositories =
                        recordsByRepository
                            .mapValues { (_, records) ->
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
            }
            else ->
                AllResolvedResponse(
                    statisticsEnabled = false,
                    interval = intervalProvider.interval
                )
        }.asSuccess()
    }

    private fun resolvedPath(repository: String, gav: String): String =
        "/$repository/${gav.trimStart('/')}"

    fun countUniqueRecords(): Long =
        statisticsRepository.countUniqueResolvedRequests()

    fun countRecords(): Long =
        statisticsRepository.countResolvedRequests()

    fun statisticsEnabled(): Reference<Boolean> =
        statisticsEnabled

    override fun getLogger(): Logger =
        journalist.logger

}

private fun DateRange.coerceToStatisticsBounds(): DateRange {
    val earliestBucketDate = STATISTICS_EARLIEST_DATE.withDayOfYear(1)
    val latestDate = LocalDate.now().plusDays(1)

    return copy(
        from = (from ?: earliestBucketDate).coerceAtLeast(earliestBucketDate),
        to = (to ?: latestDate).coerceAtMost(latestDate)
    )
}
