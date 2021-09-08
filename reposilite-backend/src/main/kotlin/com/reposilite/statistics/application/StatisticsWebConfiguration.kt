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

package com.reposilite.statistics.application

import com.reposilite.console.ConsoleFacade
import com.reposilite.journalist.Journalist
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.StatsCommand
import com.reposilite.statistics.infrastructure.SqlStatisticsRepository
import com.reposilite.statistics.infrastructure.StatisticsEndpoint
import com.reposilite.statistics.infrastructure.StatisticsHandler
import com.reposilite.web.ReposiliteRoutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.MINUTES

internal object StatisticsWebConfiguration {

    fun createFacade(journalist: Journalist, dispatcher :CoroutineDispatcher): StatisticsFacade =
        StatisticsFacade(journalist, SqlStatisticsRepository(dispatcher))

    fun initialize(statisticsFacade: StatisticsFacade, consoleFacade: ConsoleFacade, scheduler: ScheduledExecutorService, dispatcher: CoroutineDispatcher) {
        consoleFacade.registerCommand(StatsCommand(statisticsFacade))

        scheduler.scheduleWithFixedDelay({
            runBlocking {
                withContext(dispatcher) {
                    statisticsFacade.saveRecordsBulk()
                }
            }
        }, 1, 1, MINUTES)
    }

    fun routing(statisticsFacade: StatisticsFacade): Set<ReposiliteRoutes> =
        setOf(
            StatisticsEndpoint(statisticsFacade),
            StatisticsHandler(statisticsFacade),
        )

}