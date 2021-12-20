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

package com.reposilite.statistics.specification

import com.reposilite.ReposiliteSpecification
import com.reposilite.maven.api.Identifier
import com.reposilite.statistics.StatisticsFacade
import kong.unirest.Unirest.get
import kong.unirest.Unirest.put
import org.junit.jupiter.api.Assertions.assertTrue
import panda.std.Mono

internal abstract class StatisticsIntegrationSpecification : ReposiliteSpecification() {

    fun useResolvedRequest(repository: String, gav: String, content: String): Mono<Identifier> {
        val uri = "$base/$repository/$gav"
        val (token, secret) = usePredefinedTemporaryAuth()

        val putResponse = put(uri)
            .basicAuth(token, secret)
            .body(content)
            .asEmpty()

        assertTrue(putResponse.isSuccess)

        val getResponse = get(uri)
            .asEmpty()

        assertTrue(getResponse.isSuccess)

        useFacade<StatisticsFacade>().saveRecordsBulk()
        return Mono(Identifier(repository, gav))
    }

}