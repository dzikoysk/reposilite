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
import com.reposilite.statistics.api.Identifier
import com.reposilite.statistics.api.ResolvedRequestCount
import java.util.concurrent.ConcurrentHashMap

internal class InMemoryStatisticsRepository : StatisticsRepository {

    private val resolvedRequests = ConcurrentHashMap<Identifier, Long>()

    override fun incrementResolvedRequests(requests: Map<Identifier, Long>) =
        requests.forEach { (identifier, count) ->
            resolvedRequests.merge(identifier, count) { oldCount, value -> oldCount + value }
        }

    override fun findResolvedRequestsByPhrase(repository: String, phrase: String, limit: Int): List<ResolvedRequestCount> =
        resolvedRequests.asSequence()
            .filter { (identifier) -> identifier.toString().contains(phrase) }
            .sortedByDescending { (_, count) -> count }
            .take(limit)
            .map { (identifier, count) -> ResolvedRequestCount(identifier, count) }
            .toList()

    override fun countUniqueResolvedRequests(): Long =
        resolvedRequests.size.toLong()

    override fun countResolvedRecords(): Long =
        resolvedRequests.map { it.value }.sum()

}