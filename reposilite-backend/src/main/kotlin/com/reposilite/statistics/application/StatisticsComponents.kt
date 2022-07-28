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
    private val statisticsSettings: Reference<StatisticsSettings>
) : PluginComponents {

    private fun statisticsRepository(): StatisticsRepository =
        when (database) {
            null -> InMemoryStatisticsRepository()
            else -> SqlStatisticsRepository(database)
        }

    fun statisticsFacade(statisticsRepository: StatisticsRepository = statisticsRepository()): StatisticsFacade =
        StatisticsFacade(
            journalist = journalist,
            dateIntervalProvider = statisticsSettings.computed { createDateIntervalProvider(it.resolvedRequestsInterval) },
            statisticsRepository = statisticsRepository
        )

}
