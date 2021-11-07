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

import com.reposilite.Reposilite
import com.reposilite.journalist.Journalist
import com.reposilite.settings.SettingsFacade
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.StatsCommand
import com.reposilite.statistics.createDateIntervalProvider
import com.reposilite.statistics.infrastructure.SqlStatisticsRepository
import com.reposilite.statistics.infrastructure.StatisticsEndpoint
import com.reposilite.web.WebConfiguration
import com.reposilite.web.application.ReposiliteRoutes
import org.jetbrains.exposed.sql.Database
import panda.std.reactive.computed
import java.util.concurrent.TimeUnit.SECONDS

internal object StatisticsWebConfiguration : WebConfiguration {

    fun createFacade(journalist: Journalist, database: Database, settingsFacade: SettingsFacade): StatisticsFacade =
        StatisticsFacade(
            journalist,
            computed(settingsFacade.sharedConfiguration.statistics) {
                settingsFacade.sharedConfiguration.statistics.map {
                    createDateIntervalProvider(it.resolvedRequestsInterval)
                }
            },
            SqlStatisticsRepository(database)
        )

    override fun initialize(reposilite: Reposilite) {
        val statisticsFacade = reposilite.statisticsFacade

        val consoleFacade = reposilite.consoleFacade
        consoleFacade.registerCommand(StatsCommand(statisticsFacade))

        reposilite.scheduler.scheduleWithFixedDelay({
            reposilite.ioService.execute {
                statisticsFacade.saveRecordsBulk()
            }
        }, 10, 10, SECONDS)
    }

    override fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> = setOf(
        StatisticsEndpoint(reposilite.statisticsFacade)
    )

}