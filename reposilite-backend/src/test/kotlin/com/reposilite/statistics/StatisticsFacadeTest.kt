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

import com.reposilite.statistics.specification.StatisticsSpecification
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
    fun `should properly count records and unique records`() {
        // given: two different identifiers
        useResolvedIdentifier("releases", "/first", 2)
        useResolvedIdentifier("releases", "/first/second")
        useResolvedIdentifier("snapshots", "/first/second")

        // then: count should properly respect criteria of uniqueness (type & identifier)
        assertThat(statisticsFacade.countRecords()).isEqualTo(4)
        assertThat(statisticsFacade.countUniqueRecords()).isEqualTo(3)
    }

}
