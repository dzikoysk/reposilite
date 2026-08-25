/*
 * Copyright (c) 2020-2026 dzikoysk
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

@file:Suppress("FunctionName")

package com.reposilite.statistics

import com.reposilite.ExperimentalLocalSpecificationJunitExtension
import com.reposilite.ExperimentalRemoteSpecificationJunitExtension
import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.RecommendedRemoteSpecificationJunitExtension
import com.reposilite.maven.api.Identifier
import com.reposilite.statistics.api.AllResolvedResponse
import com.reposilite.statistics.api.IntervalRecord
import com.reposilite.statistics.api.RepositoryStatistics
import com.reposilite.statistics.api.ResolvedCountResponse
import com.reposilite.statistics.api.ResolvedEntriesPage
import com.reposilite.statistics.api.ResolvedEntriesResponse
import com.reposilite.statistics.api.ResolvedEntry
import com.reposilite.statistics.api.ResolvedStatisticsEntry
import com.reposilite.statistics.specification.StatisticsIntegrationSpecification
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.RoutePermission.READ
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.FORBIDDEN
import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.UNAUTHORIZED
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kong.unirest.core.Unirest.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import panda.std.component1

@ExtendWith(ExperimentalRemoteSpecificationJunitExtension::class)
internal class ExperimentalRemoteStatisticsIntegrationTest : StatisticsIntegrationTest()

@ExtendWith(RecommendedRemoteSpecificationJunitExtension::class)
internal class RemoteStatisticsIntegrationTest : StatisticsIntegrationTest()

@ExtendWith(ExperimentalLocalSpecificationJunitExtension::class)
internal class ExperimentalLocalStatisticsIntegrationTest : StatisticsIntegrationTest()

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class LocalStatisticsIntegrationTest : StatisticsIntegrationTest()

internal abstract class StatisticsIntegrationTest : StatisticsIntegrationSpecification() {

    @Test
    fun `should return unique number of requests`() {
        // given: a route to request
        val endpoint = "$base/api/statistics/resolved/unique"
        repeat(10) { useResolvedRequest("releases", "com/reposilite.jar", "content") }
        useResolvedRequests(
            mapOf(Identifier("releases", "com/reposilite.jar") to 1),
            LocalDate.now().minusMonths(1)
        )

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get(endpoint).asString()

        // then: service rejects request
        assertThat(unauthorizedResponse.status).isEqualTo(UNAUTHORIZED.code)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", listOf(MANAGER))

        // when: service is requested with valid credentials
        val response = get(endpoint)
            .basicAuth(name, secret)
            .asObject { it.contentAsString.toLong() }

        // then: service responds with valid stats data
        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).isEqualTo(1)
    }

    @Test
    fun `should return registered number of endpoint calls`() {
        // given: a route to request and check
        val (identifier) = useResolvedRequest("releases", "com/reposilite.jar", "content")
        val endpoint = "$base/api/statistics/resolved/phrase/1$identifier"

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get(endpoint).asString()

        // then: service rejects request
        assertThat(unauthorizedResponse.status).isEqualTo(UNAUTHORIZED.code)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", emptyList(), mapOf(identifier.toString() to READ))

        // when: service is requested with valid credentials
        val response = get(endpoint)
            .basicAuth(name, secret)
            .asObject(ResolvedCountResponse::class.java)

        // then: service responds with valid stats data
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.body.sum).isEqualTo(1)
        assertThat(response.body.requests[0].gav).isEqualTo(identifier.gav)
    }

    @Test
    fun `should return resolved requests without filters`() {
        // given: a recorded request
        useResolvedRequests(
            mapOf(Identifier("releases", "com/reposilite.jar") to 1),
            LocalDate.now()
        )

        // when: statistics are requested without a repository or phrase
        val requests = findResolvedRequests("", "", 20)

        // then: the unrestricted query returns recorded requests
        assertThat(requests).isEqualTo(listOf(ResolvedEntry("com/reposilite.jar", 1)))
    }

    @Test
    fun `should limit phrase results to routes visible by token`() {
        // given: two matching paths in the same repository, but only one under the token route
        useResolvedRequest("releases", "com/reposilite/app.jar", "content")
        repeat(2) {
            useResolvedRequest("releases", "other/com/reposilite/app.jar", "content")
        }
        val endpoint = "$base/api/statistics/resolved/phrase/1/releases/com/reposilite"
        val (name, secret) = useAuth("name", "secret", emptyList(), mapOf("/releases/COM/REPOSILITE" to READ))

        // when: stats service is requested with route-scoped credentials
        val response = get(endpoint)
            .basicAuth(name, secret)
            .asObject(ResolvedCountResponse::class.java)

        // then: response only includes entries covered by the token route
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.body).isEqualTo(
            ResolvedCountResponse(
                sum = 1,
                requests = listOf(ResolvedEntry("com/reposilite/app.jar", 1))
            )
        )

        val forbiddenResponse = get("$base/api/statistics/resolved/phrase/1/releases/other")
            .basicAuth(name, secret)
            .asString()

        assertThat(forbiddenResponse.status).isEqualTo(FORBIDDEN.code)
    }

    @Test
    fun `should return paginated resolved entries`() {
        // given: a few routes to request and check
        useResolvedRequests(
            mapOf(
                Identifier("releases", "com/reposilite.jar") to 2,
                Identifier("snapshots", "com/reposilite-snapshot.jar") to 1
            ),
            LocalDate.now()
        )
        val endpoint = "$base/api/statistics/resolved/entries?limit=1&phrase=reposilite"

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get(endpoint).asString()

        // then: service rejects request
        assertThat(unauthorizedResponse.status).isEqualTo(UNAUTHORIZED.code)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", listOf(MANAGER))

        // when: service is requested with valid credentials
        val response = get(endpoint)
            .basicAuth(name, secret)
            .asObject(ResolvedEntriesResponse::class.java)

        // then: service responds with a paginated entry list
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.body).isEqualTo(
            ResolvedEntriesResponse(
                page = ResolvedEntriesPage(limit = 1, offset = 0, hasMore = true, nextOffset = 1),
                entries = listOf(ResolvedStatisticsEntry("releases", "com/reposilite.jar", 2))
            )
        )
    }

    @Test
    fun `should return unfiltered entries and reject malformed pagination`() {
        // given: a recorded request and manager credentials
        useResolvedRequest("releases", "com/reposilite.jar", "content")
        val (name, secret) = useAuth("name", "secret", listOf(MANAGER))

        // when: entries are requested without optional filters
        val response = get("$base/api/statistics/resolved/entries")
            .basicAuth(name, secret)
            .asObject(ResolvedEntriesResponse::class.java)

        // then: the request succeeds and malformed pagination is rejected
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.body).isEqualTo(
            ResolvedEntriesResponse(
                page = ResolvedEntriesPage(limit = 100, offset = 0, hasMore = false, nextOffset = null),
                entries = listOf(ResolvedStatisticsEntry("releases", "com/reposilite.jar", 1))
            )
        )
        assertThat(get("$base/api/statistics/resolved/entries?limit=invalid").basicAuth(name, secret).asString().status).isEqualTo(BAD_REQUEST.code)
        assertThat(get("$base/api/statistics/resolved/entries?offset=invalid").basicAuth(name, secret).asString().status).isEqualTo(BAD_REQUEST.code)
        assertThat(get("$base/api/statistics/resolved/entries?from=invalid").basicAuth(name, secret).asString().status).isEqualTo(BAD_REQUEST.code)
        assertThat(get("$base/api/statistics/resolved/entries?to=invalid").basicAuth(name, secret).asString().status).isEqualTo(BAD_REQUEST.code)

        val today = LocalDate.now()
        val invertedRange = "from=${today.toStatisticsInstant()}&to=${today.minusDays(1).toStatisticsInstant()}"
        assertThat(get("$base/api/statistics/resolved/all?$invertedRange").basicAuth(name, secret).asString().status).isEqualTo(BAD_REQUEST.code)

        val statisticsZone = ZoneId.systemDefault()
        val invertedSameDayRange = "from=${today.atTime(20, 0).atZone(statisticsZone).toInstant()}&to=${today.atTime(10, 0).atZone(statisticsZone).toInstant()}"
        assertThat(get("$base/api/statistics/resolved/all?$invertedSameDayRange").basicAuth(name, secret).asString().status).isEqualTo(BAD_REQUEST.code)

        val extremeRangeResponse = get("$base/api/statistics/resolved/all")
            .queryString("from", Instant.MIN.toString())
            .queryString("to", Instant.MAX.toString())
            .basicAuth(name, secret)
            .asString()
        assertThat(extremeRangeResponse.status).isEqualTo(BAD_REQUEST.code)
    }

    @Test
    fun `should treat search phrases as case-insensitive literals`() {
        // given: paths that only wildcard matching would consider equivalent
        useResolvedRequests(
            mapOf(
                Identifier("releases", "com/Literal%_Match.jar") to 1,
                Identifier("releases", "com/LiteralXXMatch.jar") to 1
            ),
            LocalDate.now()
        )
        val (name, secret) = useAuth("name", "secret", listOf(MANAGER))

        // when: both statistics search endpoints receive an encoded literal phrase
        val phraseResponse = get("$base/api/statistics/resolved/phrase/10/releases/LITERAL%25_")
            .basicAuth(name, secret)
            .asObject(ResolvedCountResponse::class.java)
        val entriesResponse = get("$base/api/statistics/resolved/entries?phrase=LITERAL%25_")
            .basicAuth(name, secret)
            .asObject(ResolvedEntriesResponse::class.java)

        // then: only the path containing the literal characters is returned
        assertThat(phraseResponse.status).isEqualTo(OK.code)
        assertThat(phraseResponse.body).isEqualTo(
            ResolvedCountResponse(
                sum = 1,
                requests = listOf(ResolvedEntry("com/Literal%_Match.jar", 1))
            )
        )
        assertThat(entriesResponse.status).isEqualTo(OK.code)
        assertThat(entriesResponse.body).isEqualTo(
            ResolvedEntriesResponse(
                page = ResolvedEntriesPage(limit = 100, offset = 0, hasMore = false, nextOffset = null),
                entries = listOf(ResolvedStatisticsEntry("releases", "com/Literal%_Match.jar", 1))
            )
        )
    }

    @Test
    fun `should return time-series`() {
        // given: a database with some requests
        val today = LocalDate.now()
        repeat(2) { // repeat 2 times to verify aggregation
            repeat(24) { index -> // 24 months
                val date = today
                    .minusMonths(index.toLong())
                    .withDayOfMonth(1)

                useResolvedRequests(
                    requests = mapOf(
                        Identifier("releases", "/com/reposilite/1.0.0/reposilite-1.0.0.jar") to index.toLong(),
                        Identifier("snapshots", "/com/reposilite/1.0.0-SNAPSHOT/reposilite-1.0.0-SNAPSHOT.jar") to index.toLong()
                    ),
                    date = date
                )
            }
        }
        useResolvedRequests(
            mapOf(Identifier("releases", "/old/only.jar") to 100),
            today.minusYears(2)
        )

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get("$base/api/statistics/resolved/all").asString()

        // then: service rejects request
        assertThat(unauthorizedResponse.status).isEqualTo(UNAUTHORIZED.code)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", listOf(MANAGER))

        // when: service is requested with valid credentials
        val response = get("$base/api/statistics/resolved/all")
            .basicAuth(name, secret)
            .asObject(AllResolvedResponse::class.java)
        val quarterFrom = today.minusMonths(2).withDayOfMonth(1).toStatisticsInstant()
        val todayInstant = today.toStatisticsInstant()
        val quarterResponse = get("$base/api/statistics/resolved/all?from=$quarterFrom&to=$todayInstant")
            .basicAuth(name, secret)
            .asObject(AllResolvedResponse::class.java)
        val previousMonth = today.minusMonths(1).withDayOfMonth(1)
        val partialPeriodQuery = "from=${previousMonth.plusDays(1).toStatisticsInstant()}&to=${previousMonth.plusMonths(1).minusDays(1).toStatisticsInstant()}"
        val partialPeriodResponse = get("$base/api/statistics/resolved/all?$partialPeriodQuery")
            .basicAuth(name, secret)
            .asObject(AllResolvedResponse::class.java)
        val allTimeResponse = get("$base/api/statistics/resolved/all?to=$todayInstant")
            .basicAuth(name, secret)
            .asObject(AllResolvedResponse::class.java)

        // then: service should preserve its default while supporting arbitrary and open-ended date ranges
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.body).isEqualTo(
            AllResolvedResponse(
                repositories = listOf("releases", "snapshots")
                    .map { repository ->
                        RepositoryStatistics(
                            name = repository,
                            data = (0..11)
                                .map { index ->
                                    IntervalRecord(
                                        date = today.minusMonths(index.toLong()).withDayOfMonth(1).toUTCMillis(),
                                        count = 2L * index
                                    )
                                }
                                .sortedBy { it.date }
                        )
                    }
                    .sortedBy { it.name }
            )
        )
        assertThat(quarterResponse.status).isEqualTo(OK.code)
        assertThat(quarterResponse.body.repositories).allSatisfy { repository ->
            assertThat(repository.data).hasSize(3)
            assertThat(repository.data.sumOf { it.count }).isEqualTo(6)
        }
        assertThat(partialPeriodResponse.status).isEqualTo(OK.code)
        assertThat(partialPeriodResponse.body.repositories).isEmpty()
        assertThat(allTimeResponse.status).isEqualTo(OK.code)
        assertThat(allTimeResponse.body.repositories.associate { it.name to it.data.sumOf(IntervalRecord::count) })
            .containsExactlyInAnyOrderEntriesOf(mapOf("releases" to 652L, "snapshots" to 552L))

        val entries = get("$base/api/statistics/resolved/entries?repository=releases&phrase=reposilite")
            .basicAuth(name, secret)
            .asObject(ResolvedEntriesResponse::class.java)
        val oldEntries = get("$base/api/statistics/resolved/entries?phrase=old")
            .basicAuth(name, secret)
            .asObject(ResolvedEntriesResponse::class.java)
        val allTimeOldEntries = get("$base/api/statistics/resolved/entries?phrase=old&to=$todayInstant")
            .basicAuth(name, secret)
            .asObject(ResolvedEntriesResponse::class.java)
        val partialPeriodEntries = get("$base/api/statistics/resolved/entries?phrase=reposilite&$partialPeriodQuery")
            .basicAuth(name, secret)
            .asObject(ResolvedEntriesResponse::class.java)

        assertThat(entries.status).isEqualTo(OK.code)
        assertThat(entries.body).isEqualTo(
            ResolvedEntriesResponse(
                page = ResolvedEntriesPage(limit = 100, offset = 0, hasMore = false, nextOffset = null),
                entries = listOf(
                    ResolvedStatisticsEntry("releases", "/com/reposilite/1.0.0/reposilite-1.0.0.jar", 132)
                )
            )
        )
        assertThat(oldEntries.status).isEqualTo(OK.code)
        assertThat(oldEntries.body).isEqualTo(
            ResolvedEntriesResponse(
                page = ResolvedEntriesPage(limit = 100, offset = 0, hasMore = false, nextOffset = null),
                entries = emptyList()
            )
        )
        assertThat(allTimeOldEntries.status).isEqualTo(OK.code)
        assertThat(allTimeOldEntries.body.entries).containsExactly(
            ResolvedStatisticsEntry("releases", "/old/only.jar", 100)
        )
        assertThat(partialPeriodEntries.status).isEqualTo(OK.code)
        assertThat(partialPeriodEntries.body.entries).isEmpty()
    }

}

private fun LocalDate.toStatisticsInstant() =
    atStartOfDay(ZoneId.systemDefault()).toInstant()
