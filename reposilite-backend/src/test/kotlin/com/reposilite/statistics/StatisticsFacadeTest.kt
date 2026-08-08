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

import com.reposilite.maven.api.Identifier
import com.reposilite.statistics.api.AllResolvedResponse
import com.reposilite.statistics.api.IntervalRecord
import com.reposilite.statistics.api.RepositoryStatistics
import com.reposilite.statistics.api.ResolvedCountResponse
import com.reposilite.statistics.api.ResolvedEntriesPage
import com.reposilite.statistics.api.ResolvedEntriesResponse
import com.reposilite.statistics.api.ResolvedEntry
import com.reposilite.statistics.api.ResolvedRequestsInterval.DAILY
import com.reposilite.statistics.api.ResolvedStatisticsEntry
import com.reposilite.statistics.specification.StatisticsSpecification
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.api.CreateAccessTokenRequest
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk

internal class StatisticsFacadeTest : StatisticsSpecification() {

    @Test
    fun `should increase records after saving the bulk`() {
        // given: an uri to stored request
        val (identifier, count) = useResolvedIdentifier("releases", "/panda-lang/reposilite", 2)
        val (repository, gav) = identifier

        // when: the given phrase is requested
        val result = statisticsFacade.findResolvedRequestsByPhrase(repository, gav)

        // then: it should be properly stored in repository as a single record
        val response = assertOk(result)
        assertThat(response.sum).isEqualTo(count)
        assertThat(response.requests.size).isEqualTo(1)
        assertThat(response.requests[0].gav).isEqualTo(gav)
    }

    @Test
    fun `should find record by given phrase`() {
        // given: a requested uri and a phrase to search for
        val (identifier, count) = useResolvedIdentifier("releases", "/panda-lang/reposilite")
        val phrase = "reposilite"

        // when: the given phrase is requested
        val result = statisticsFacade.findResolvedRequestsByPhrase(identifier.repository, phrase)

        // then: the phrase should be found
        val response = assertOk(result)
        assertThat(response.sum).isEqualTo(count)
        assertThat(response.requests[0].gav).isEqualTo(identifier.gav)
    }

    @Test
    fun `should reject invalid resolved requests page size`() {
        assertThat(statisticsFacade.findResolvedRequestsByPhrase("releases", "/", 0).isErr).isTrue()
        assertThat(statisticsFacade.findResolvedRequestsByPhrase("releases", "/", -1).isErr).isTrue()
    }

    @Test
    fun `should properly count records and unique records`() {
        // given: two different identifiers
        useResolvedIdentifier("releases", "/first", 2)
        useResolvedIdentifier("releases", "/first/second")
        useResolvedIdentifier("snapshots", "/first/second")

        // then: count should properly respect criteria of uniqueness (type & identifier)
        assertThat(statisticsFacade.countRecords()).isEqualTo(4)
        assertThat(statisticsFacade.countUniqueRecords()).isEqualTo(3)
    }

    @Test
    fun `should find resolved entries across repositories`() {
        // given: resolved entries in a few repositories
        useResolvedIdentifier("releases", "/first", 5)
        useResolvedIdentifier("snapshots", "/second", 3)
        useResolvedIdentifier("private", "/third", 1)

        // when: the first page of resolved entries is requested
        val result = statisticsFacade.findResolvedEntries(
            repository = null,
            phrase = "/",
            limit = 2,
            offset = 0
        )

        // then: it should return sorted entries with page metadata
        val response = assertOk(result)
        assertThat(response).isEqualTo(
            ResolvedEntriesResponse(
                page = ResolvedEntriesPage(limit = 2, offset = 0, hasMore = true, nextOffset = 2),
                entries = listOf(
                    ResolvedStatisticsEntry("releases", "/first", 5),
                    ResolvedStatisticsEntry("snapshots", "/second", 3)
                )
            )
        )
    }

    @Test
    fun `should find resolved entries in selected repository`() {
        // given: same path resolved in two repositories
        useResolvedIdentifier("releases", "/first", 5)
        useResolvedIdentifier("snapshots", "/first", 3)

        // when: selected repository is requested
        val result = statisticsFacade.findResolvedEntries(
            repository = "snapshots",
            phrase = "first"
        )

        // then: it should only return entries from that repository
        val response = assertOk(result)
        assertThat(response).isEqualTo(
            ResolvedEntriesResponse(
                page = ResolvedEntriesPage(limit = 100, offset = 0, hasMore = false, nextOffset = null),
                entries = listOf(ResolvedStatisticsEntry("snapshots", "/first", 3))
            )
        )
    }

    @Test
    fun `should aggregate in-memory records within the visible window`() {
        // given: the same literal path recorded on multiple dates, including an expired record
        val identifier = Identifier("releases", "com/Literal%_Match.jar")
        val today = LocalDate.now()
        useResolvedRequests(mapOf(identifier to 2), today)
        useResolvedRequests(mapOf(identifier to 3), today.minusDays(1))
        useResolvedRequests(mapOf(identifier to 7), today.minusYears(2))
        useResolvedRequests(
            mapOf(Identifier("releases", "com/LiteralXXMatch.jar") to 20),
            today
        )

        // when: entries and all-time phrase results are requested using different casing
        val entries = assertOk(statisticsFacade.findResolvedEntries(null, "%_MATCH"))
        val phrase = assertOk(statisticsFacade.findResolvedRequestsByPhrase("releases", "%_MATCH"))
        val all = assertOk(statisticsFacade.getAllResolvedStatistics())

        // then: current entries are aggregated, while the existing phrase API remains all-time
        assertThat(entries).isEqualTo(
            ResolvedEntriesResponse(
                page = ResolvedEntriesPage(limit = 100, offset = 0, hasMore = false, nextOffset = null),
                entries = listOf(ResolvedStatisticsEntry("releases", "com/Literal%_Match.jar", 5))
            )
        )
        assertThat(phrase).isEqualTo(
            ResolvedCountResponse(
                sum = 12,
                requests = listOf(ResolvedEntry("com/Literal%_Match.jar", 12))
            )
        )
        assertThat(all).isEqualTo(
            AllResolvedResponse(
                interval = DAILY,
                repositories = listOf(
                    RepositoryStatistics(
                        name = "releases",
                        data = DailyDateIntervalProvider.createTimeSeries()
                            .associateWith { date ->
                                when (date) {
                                    today -> 22L
                                    today.minusDays(1) -> 3L
                                    else -> 0L
                                }
                            }
                            .map { (date, count) -> IntervalRecord(date.toUTCMillis(), count) }
                            .sortedBy { it.date }
                    )
                )
            )
        )
    }

    @Test
    fun `should limit phrase results to accessible prefixes`() {
        // given: matching entries both inside and outside of the accessible prefix
        useResolvedIdentifier("releases", "com/reposilite/app.jar", 1)
        useResolvedIdentifier("releases", "other/com/reposilite/app.jar", 2)
        val accessToken = useAccessToken(
            "reader",
            setOf(CreateAccessTokenRequest.Route("/releases/com/reposilite", setOf(READ)))
        )

        // when: a limited search is performed with an accessible prefix
        val response = assertOk(statisticsFacade.findResolvedRequestsByPhrase(
            repository = "releases",
            phrase = "com/reposilite",
            limit = 1,
            accessToken = accessToken.identifier
        ))

        // then: inaccessible entries do not consume the result limit
        assertThat(response).isEqualTo(
            ResolvedCountResponse(
                sum = 1,
                requests = listOf(ResolvedEntry("com/reposilite/app.jar", 1))
            )
        )
        assertThat(statisticsFacade.findResolvedRequestsByPhrase(
            repository = "releases",
            phrase = "other/com/reposilite",
            accessToken = accessToken.identifier
        ).isErr).isTrue()
    }

}
