package com.reposilite.statistics.specification

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.api.RecordType
import com.reposilite.statistics.infrastructure.InMemoryStatisticsRepository

internal open class StatisticsSpecification {

    private val logger = InMemoryLogger()
    protected val statisticsFacade = StatisticsFacade(logger, InMemoryStatisticsRepository())

    protected suspend fun useRecordedIdentifier(type: RecordType, identifier: String, times: Int = 1): Pair<RecordType, String> {
        repeat(times) {
            increaseAndSave(type, identifier)
        }

        return Pair(type, identifier)
    }

    protected suspend fun increaseAndSave(type: RecordType, identifier: String) {
        statisticsFacade.increaseRecord(type, identifier)
        statisticsFacade.saveRecordsBulk()
    }

}