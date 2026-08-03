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

package com.reposilite.statistics.specification

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.maven.api.Identifier
import com.reposilite.statistics.DailyDateIntervalProvider
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.api.IncrementResolvedRequest
import com.reposilite.statistics.infrastructure.InMemoryStatisticsRepository
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.application.AccessTokenComponents
import java.time.LocalDate
import panda.std.reactive.Reference
import panda.std.reactive.toReference

internal open class StatisticsSpecification {

    private val logger = InMemoryLogger()

    private val accessTokenFacade = AccessTokenComponents(
        journalist = logger,
        database = null
    ).accessTokenFacade()

    private val statisticsRepository = InMemoryStatisticsRepository()

    protected val statisticsFacade = StatisticsFacade(
        journalist = logger,
        statisticsEnabled = Reference.reference(true),
        dateIntervalProvider = DailyDateIntervalProvider.toReference(),
        statisticsRepository = statisticsRepository,
        accessTokenFacade = accessTokenFacade
    )

    protected fun useResolvedIdentifier(repository: String, gav: String, count: Long = 1): Pair<Identifier, Long> {
        val identifier = Identifier(repository, gav)
        increaseAndSave(identifier, count)
        return identifier to count
    }

    protected fun useResolvedRequests(requests: Map<Identifier, Long>, date: LocalDate) {
        statisticsRepository.incrementResolvedRequests(requests, date)
    }

    protected fun useAccessToken(name: String, routes: Set<CreateAccessTokenRequest.Route>) =
        accessTokenFacade.createAccessToken(
            CreateAccessTokenRequest(type = PERSISTENT, name = name, routes = routes)
        ).accessToken

    private fun increaseAndSave(identifier: Identifier, count: Long) {
        statisticsFacade.incrementResolvedRequest(IncrementResolvedRequest(identifier, count))
        statisticsFacade.saveRecordsBulk()
    }

}
