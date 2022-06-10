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

@file:Suppress("FunctionName")

package com.reposilite.statistics

import com.reposilite.ReposiliteExperimentalLocalIntegrationJunitExtension
import com.reposilite.ReposiliteExperimentalRemoteIntegrationJunitExtension
import com.reposilite.ReposiliteLocalIntegrationJunitExtension
import com.reposilite.ReposiliteRemoteIntegrationJunitExtension
import com.reposilite.statistics.api.ResolvedCountResponse
import com.reposilite.statistics.specification.StatisticsIntegrationSpecification
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.RoutePermission.READ
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.Unirest.get
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import panda.std.component1

@ExtendWith(ReposiliteExperimentalRemoteIntegrationJunitExtension::class)
internal class ExperimentalRemoteStatisticsIntegrationTest : StatisticsIntegrationTest()

@ExtendWith(ReposiliteRemoteIntegrationJunitExtension::class)
internal class RemoteStatisticsIntegrationTest : StatisticsIntegrationTest()

@ExtendWith(ReposiliteExperimentalLocalIntegrationJunitExtension::class)
internal class ExperimentalLocalStatisticsIntegrationTest : StatisticsIntegrationTest()

@ExtendWith(ReposiliteLocalIntegrationJunitExtension::class)
internal class LocalStatisticsIntegrationTest : StatisticsIntegrationTest()

internal abstract class StatisticsIntegrationTest : StatisticsIntegrationSpecification() {

    @Test
    fun `should return registered number of endpoint calls`() = runBlocking {
        // given: a route to request and check
        val (identifier) = useResolvedRequest("releases", "com/reposilite.jar", "content")
        val endpoint = "$base/api/statistics/resolved/phrase/1$identifier"

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get(endpoint).asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.status, unauthorizedResponse.status)

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
    fun `should return unique number of requests`() {
        // given: a route to request and check
        val endpoint = "$base/api/statistics/resolved/unique"
        repeat(10) { useResolvedRequest("releases", "com/reposilite.jar", "content") }

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get(endpoint).asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.status, unauthorizedResponse.status)

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

}