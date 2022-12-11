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

package com.reposilite.statistics.infrastructure

import com.reposilite.maven.api.Identifier
import com.reposilite.statistics.StatisticsRepository
import com.reposilite.statistics.api.IntervalRecord
import com.reposilite.statistics.api.RepositoryStatistics
import com.reposilite.statistics.api.ResolvedEntry
import java.time.LocalDate
import java.time.ZoneId

internal class InMemoryStatisticsRepository : StatisticsRepository {

    private data class ResolvedRequest(
        val identifier: Identifier,
        val date: LocalDate,
        var count: Long
    )

    private val resolvedRequests = ArrayList<ResolvedRequest>()

    override fun incrementResolvedRequests(requests: Map<Identifier, Long>, date: LocalDate) =
        requests.forEach { (identifier, count) ->
            resolvedRequests
                .firstOrNull { it.identifier == identifier }
                ?.let { it.count += count }
                ?: resolvedRequests.add(ResolvedRequest(identifier, date, count))
        }

    override fun findResolvedRequestsByPhrase(repository: String, phrase: String, limit: Int): List<ResolvedEntry> =
        resolvedRequests.asSequence()
            .filter { it.identifier.repository == repository }
            .filter { (identifier) -> identifier.toString().contains(phrase) }
            .sortedByDescending { (_, count) -> count }
            .take(limit)
            .map { (identifier, _, count) -> ResolvedEntry(identifier.gav, count) }
            .toList()

    override fun getAllResolvedRequestsPerRepositoryAsTimeseries(): Collection<RepositoryStatistics> =
        resolvedRequests
            .groupBy(
                keySelector = { it.identifier.repository },
                valueTransform = { IntervalRecord(it.date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000, it.count) }
            )
            .map { RepositoryStatistics(it.key, it.value) }

    override fun countUniqueResolvedRequests(): Long =
        resolvedRequests.size.toLong()

    override fun countResolvedRequests(): Long =
        resolvedRequests.sumOf { it.count }

}
