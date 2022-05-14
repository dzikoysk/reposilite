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

package com.reposilite.statistics.application

import com.reposilite.Reposilite
import com.reposilite.console.ConsoleFacade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.settings.shared.SharedConfigurationFacade
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.StatsCommand
import com.reposilite.statistics.createDateIntervalProvider
import com.reposilite.statistics.infrastructure.SqlStatisticsRepository
import com.reposilite.statistics.infrastructure.StatisticsEndpoint
import com.reposilite.web.api.RoutingSetupEvent
import java.util.concurrent.TimeUnit.SECONDS

@Plugin(name = "statistics", dependencies = ["shared-configuration", "console"])
internal class StatisticsPlugin : ReposilitePlugin() {

    override fun initialize(): StatisticsFacade {
        val reposilite = facade<Reposilite>()
        val consoleFacade = facade<ConsoleFacade>()

        val settingsFacade = facade<SharedConfigurationFacade>()
        val statisticsSettings = settingsFacade.createDomainSettings(StatisticsSettings())

        val statisticsFacade = StatisticsFacade(
            journalist = this,
            dateIntervalProvider = statisticsSettings.computed { createDateIntervalProvider(it.resolvedRequestsInterval) },
            statisticsRepository = SqlStatisticsRepository(reposilite.database)
        )

        consoleFacade.registerCommand(StatsCommand(statisticsFacade))

        event { _: ReposiliteInitializeEvent ->
            reposilite.scheduler.scheduleWithFixedDelay({
                reposilite.ioService.execute {
                    statisticsFacade.saveRecordsBulk()
                }
            }, 10, 10, SECONDS)
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(StatisticsEndpoint(statisticsFacade))
        }

        return statisticsFacade
    }

}
