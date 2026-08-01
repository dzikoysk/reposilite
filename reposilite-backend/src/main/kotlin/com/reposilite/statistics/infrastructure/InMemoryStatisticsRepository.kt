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

package com.reposilite.statistics.infrastructure

import com.reposilite.maven.api.Identifier
import com.reposilite.statistics.StatisticsRepository
import com.reposilite.statistics.api.ResolvedEntry
import com.reposilite.statistics.api.ResolvedStatisticsEntry
import java.time.LocalDate

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
                .firstOrNull { it.identifier == identifier && it.date == date }
                ?.let { it.count += count }
                ?: resolvedRequests.add(ResolvedRequest(identifier, date, count))
        }

    override fun findResolvedRequestsByPhrase(
        repository: String,
        phrase: String,
        limit: Int,
        accessibleGavPrefixes: Set<String>?
    ): List<ResolvedEntry> =
        resolvedRequests.asSequence()
            .filter { repository.isEmpty() || it.identifier.repository == repository }
            .filter { (identifier) -> phrase.isEmpty() || identifier.gav.contains(phrase, ignoreCase = true) }
            .filter { (identifier) -> accessibleGavPrefixes == null || accessibleGavPrefixes.any { identifier.gav.startsWith(it, ignoreCase = true) } }
            .groupBy { it.identifier }
            .map { (identifier, records) -> ResolvedEntry(identifier.gav, records.sumOf { it.count }) }
            .sortedByDescending { it.count }
            .take(limit)

    override fun findResolvedEntries(
        repository: String?,
        phrase: String,
        from: LocalDate,
        limit: Int,
        offset: Long
    ): List<ResolvedStatisticsEntry> =
        if (offset > Int.MAX_VALUE) {
            emptyList()
        } else {
            resolvedRequests.asSequence()
                .filter { it.date >= from }
                .filter { repository == null || it.identifier.repository == repository }
                .filter { (identifier) -> phrase.isEmpty() || identifier.gav.contains(phrase, ignoreCase = true) }
                .groupBy { it.identifier }
                .map { (identifier, records) -> ResolvedStatisticsEntry(identifier.repository, identifier.gav, records.sumOf { it.count }) }
                .sortedWith(compareByDescending<ResolvedStatisticsEntry> { it.count }.thenBy { it.repository }.thenBy { it.path })
                .drop(offset.toInt())
                .take(limit)
        }

    override fun getAllResolvedRequestsPerRepositoryAsTimeSeries(from: LocalDate): Map<String, Map<LocalDate, Long>> =
        resolvedRequests
            .filter { it.date >= from }
            .groupBy { it.identifier.repository }
            .mapValues { (_, records) ->
                records.groupingBy { it.date }.fold(0L) { count, record -> count + record.count }
            }

    override fun countUniqueResolvedRequests(): Long =
        resolvedRequests.asSequence().map { it.identifier }.distinct().count().toLong()

    override fun countResolvedRequests(): Long =
        resolvedRequests.sumOf { it.count }

}
