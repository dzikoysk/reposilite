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

package com.reposilite.statistics.application

import com.reposilite.journalist.Journalist
import com.reposilite.plugin.api.PluginComponents
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.StatisticsRepository
import com.reposilite.statistics.createDateIntervalProvider
import com.reposilite.statistics.infrastructure.InMemoryStatisticsRepository
import com.reposilite.statistics.infrastructure.SqlStatisticsRepository
import org.jetbrains.exposed.sql.Database
import panda.std.reactive.Reference

class StatisticsComponents(
    private val journalist: Journalist,
    private val database: Database?,
    private val runMigrations: Boolean,
    private val statisticsSettings: Reference<StatisticsSettings>
) : PluginComponents {

    private fun statisticsRepository(): StatisticsRepository =
        when (database) {
            null -> InMemoryStatisticsRepository()
            else -> SqlStatisticsRepository(database, journalist, runMigrations)
        }

    fun statisticsFacade(statisticsRepository: StatisticsRepository = statisticsRepository()): StatisticsFacade =
        StatisticsFacade(
            journalist = journalist,
            statisticsEnabled = statisticsSettings.computed { it.enabled },
            dateIntervalProvider = statisticsSettings.computed { createDateIntervalProvider(it.resolvedRequestsInterval) },
            statisticsRepository = statisticsRepository
        )

}
