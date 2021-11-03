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

package com.reposilite.statistics

import com.reposilite.statistics.api.ResolvedCountResponse
import com.reposilite.statistics.specification.StatisticsIntegrationSpecification
import com.reposilite.token.api.RoutePermission.READ
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.Unirest.get
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal abstract class StatisticsIntegrationTest : StatisticsIntegrationSpecification() {

    @Test
    fun `should return registered amount of endpoint calls`() = runBlocking {
        // given: a route to request and check
        val route = useRecordedRecord("/releases/com/reposilite")
        val endpoint = "$base/api/statistics/count/request$route"

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get(endpoint).asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.status, unauthorizedResponse.status)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", mapOf(route to READ))

        // when: service is requested with valid credentials
        val response = get(endpoint)
            .basicAuth(name, secret)
            .asObject(ResolvedCountResponse::class.java)

        // then: service responds with valid stats data
        assertTrue(response.isSuccess)
        assertEquals(1, response.body.sum)
        assertEquals(route, response.body.records[0].identifier)
    }

}