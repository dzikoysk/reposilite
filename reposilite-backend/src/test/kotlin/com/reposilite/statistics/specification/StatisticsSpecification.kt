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

package com.reposilite.statistics.specification

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.maven.api.Identifier
import com.reposilite.statistics.DailyDateIntervalProvider
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.api.IncrementResolvedRequest
import com.reposilite.statistics.infrastructure.InMemoryStatisticsRepository
import panda.std.reactive.toReference

internal open class StatisticsSpecification {

    private val logger = InMemoryLogger()
    protected val statisticsFacade = StatisticsFacade(logger, DailyDateIntervalProvider.toReference(), InMemoryStatisticsRepository())

    protected fun useResolvedIdentifier(repository: String, gav: String, count: Long = 1): Pair<Identifier, Long> {
        val identifier = Identifier(repository, gav)
        increaseAndSave(identifier, count)
        return Pair(identifier, count)
    }

    private fun increaseAndSave(identifier: Identifier, count: Long) {
        statisticsFacade.incrementResolvedRequest(IncrementResolvedRequest(identifier, count))
        statisticsFacade.saveRecordsBulk()
    }

}