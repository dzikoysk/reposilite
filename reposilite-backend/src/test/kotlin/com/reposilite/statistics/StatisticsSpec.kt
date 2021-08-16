package com.reposilite.statistics

import com.reposilite.statistics.api.RecordType
import com.reposilite.statistics.infrastructure.InMemoryStatisticsRepository
import net.dzikoysk.dynamiclogger.backend.InMemoryLogger

internal open class StatisticsSpec {

    private val logger = InMemoryLogger()
    protected val statisticsFacade = StatisticsFacade(logger, InMemoryStatisticsRepository())

    protected fun increaseAndSave(type: RecordType, identifier: String) {
        statisticsFacade.increaseRecord(type, identifier)
        statisticsFacade.saveRecordsBulk()
    }

}