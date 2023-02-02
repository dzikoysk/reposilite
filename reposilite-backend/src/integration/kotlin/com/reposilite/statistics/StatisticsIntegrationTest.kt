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

@file:Suppress("FunctionName")

package com.reposilite.statistics

import com.reposilite.maven.api.Identifier
import com.reposilite.specification.ExperimentalLocalSpecificationJunitExtension
import com.reposilite.specification.ExperimentalRemoteSpecficiationJunitExtension
import com.reposilite.specification.LocalSpecificationJunitExtension
import com.reposilite.specification.RemoteSpecificationJunitExtension
import com.reposilite.statistics.api.AllResolvedResponse
import com.reposilite.statistics.api.IntervalRecord
import com.reposilite.statistics.api.RepositoryStatistics
import com.reposilite.statistics.api.ResolvedCountResponse
import com.reposilite.statistics.infrastructure.SqlStatisticsRepository
import com.reposilite.statistics.specification.StatisticsIntegrationSpecification
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.RoutePermission.READ
import io.javalin.http.HttpStatus.UNAUTHORIZED
import kong.unirest.Unirest.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import panda.std.component1
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@ExtendWith(ExperimentalRemoteSpecficiationJunitExtension::class)
internal class ExperimentalRemoteStatisticsIntegrationTest : StatisticsIntegrationTest()

@ExtendWith(RemoteSpecificationJunitExtension::class)
internal class RemoteStatisticsIntegrationTest : StatisticsIntegrationTest()

@ExtendWith(ExperimentalLocalSpecificationJunitExtension::class)
internal class ExperimentalLocalStatisticsIntegrationTest : StatisticsIntegrationTest()

@ExtendWith(LocalSpecificationJunitExtension::class)
internal class LocalStatisticsIntegrationTest : StatisticsIntegrationTest()

internal abstract class StatisticsIntegrationTest : StatisticsIntegrationSpecification() {

    @Test
    fun `should return unique number of requests`() {
        // given: a route to request
        val endpoint = "$base/api/statistics/resolved/unique"
        repeat(10) { useResolvedRequest("releases", "com/reposilite.jar", "content") }

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get(endpoint).asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.code, unauthorizedResponse.status)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", listOf(MANAGER))

        // when: service is requested with valid credentials
        val response = get(endpoint)
            .basicAuth(name, secret)
            .asObject { it.contentAsString.toLong() }

        // then: service responds with valid stats data
        assertEquals(200, response.status)
        assertEquals(1, response.body)
    }

    @Test
    fun `should return registered number of endpoint calls`() {
        // given: a route to request and check
        val (identifier) = useResolvedRequest("releases", "com/reposilite.jar", "content")
        val endpoint = "$base/api/statistics/resolved/phrase/1$identifier"

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get(endpoint).asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.code, unauthorizedResponse.status)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", emptyList(),  mapOf(identifier.toString() to READ))

        // when: service is requested with valid credentials
        val response = get(endpoint)
            .basicAuth(name, secret)
            .asObject(ResolvedCountResponse::class.java)

        // then: service responds with valid stats data
        assertEquals(200, response.status)
        assertEquals(1, response.body.sum)
        assertEquals(identifier.gav, response.body.requests[0].gav)
    }

    @Test
    fun `should return time-series`() {
        // given: a database with some requests
        val hackyDatabaseStateAccessor = SqlStatisticsRepository(reposilite.database, reposilite.journalist, false)

        repeat(2) { // repeat 2 times to verify aggregation
            repeat(24) { index -> // 24 months
                val date = LocalDate.now()
                    .minusMonths(index.toLong())
                    .withDayOfMonth(1)

                hackyDatabaseStateAccessor.incrementResolvedRequests(
                    requests = mapOf(
                        Identifier("releases", "/com/reposilite/1.0.0/reposilite-1.0.0.jar") to index.toLong(),
                        Identifier("snapshots", "/com/reposilite/1.0.0-SNAPSHOT/reposilite-1.0.0-SNAPSHOT.jar") to index.toLong()
                    ),
                    date = date
                )
            }
        }

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get("$base/api/statistics/resolved/all").asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.code, unauthorizedResponse.status)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", listOf(MANAGER))

        // when: service is requested with valid credentials
        val response = get("$base/api/statistics/resolved/all")
            .basicAuth(name, secret)
            .asObject(AllResolvedResponse::class.java)

        // then: service should respond with time-series not older than a year
        assertEquals(200, response.status)

        assertThat(response.body).isEqualTo(
            AllResolvedResponse(
                repositories = listOf("releases", "snapshots")
                    .map { repository ->
                        RepositoryStatistics(
                            name = repository,
                            data = (0..ChronoUnit.MONTHS.between(LocalDate.now().minusYears(1), LocalDate.now())) // may be 12 or 13 months
                                .map { index ->
                                    IntervalRecord(
                                        date = LocalDate.now().minusMonths(index).withDayOfMonth(1).toUTCMillis(),
                                        count = 2L * index
                                    )
                                }
                                .sortedBy { it.date }
                        )
                    }
                    .sortedBy { it.name }
            )
        )
    }

}