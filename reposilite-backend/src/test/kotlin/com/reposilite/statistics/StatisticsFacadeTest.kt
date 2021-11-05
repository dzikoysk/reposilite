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

import com.reposilite.statistics.api.RecordType.REQUEST
import com.reposilite.statistics.api.RecordType.UNKNOWN
import com.reposilite.statistics.specification.StatisticsSpecification
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk

internal class StatisticsFacadeTest : StatisticsSpecification() {

    @Test
    fun `should increase records after saving the bulk`() = runBlocking {
        // given: an uri to stored request
        val (type, uri) = useRecordedIdentifier(REQUEST, "/panda-lang/reposilite", 2)

        // when: the given phrase is requested
        val result = statisticsFacade.findResolvedRequestsByPhrase(type.name, uri)

        // then: it should be properly stored in repository as a single record
        val response = assertOk(result)
        assertEquals(2, response.count)
        assertEquals(1, response.requests.size)
        assertEquals(uri, response.requests[0].identifier)
    }

    @Test
    fun `should find record by given phrase`() = runBlocking {
        // given: a requested uri and a phrase to search for
        val (type, uri) = useRecordedIdentifier(REQUEST, "/panda-lang/reposilite")
        val phrase = "reposilite"

        // when: the given phrase is requested
        val result = statisticsFacade.findResolvedRequestsByPhrase(type.name, phrase)

        // then: the phrase should be found
        val response = assertOk(result)
        assertEquals(1, response.count)
        assertEquals(uri, response.requests[0].identifier)
    }

    @Test
    fun `should properly count records and unique records`() = runBlocking {
        // given: two different identifiers
        useRecordedIdentifier(REQUEST, "/first", 2)
        useRecordedIdentifier(REQUEST, "/first/second")
        useRecordedIdentifier(UNKNOWN, "/first/second")

        // then: count should properly respect criteria of uniqueness (type & identifier)
        assertEquals(4, statisticsFacade.countRecords())
        assertEquals(3, statisticsFacade.countUniqueRecords())
    }

}