package org.panda_lang.reposilite.statistics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.statistics.api.RecordType.REQUEST

internal class StatisticsFacadeTest : StatisticsSpec() {

    @Test
    fun `should increase records after saving the bulk`() {
        // given: an uri to request
        val uri = "/x/y/z"

        // when: the given uri is requested twice
        statisticsFacade.increaseRecord(REQUEST, uri)
        statisticsFacade.increaseRecord(REQUEST, uri)
        statisticsFacade.saveRecordsBulk()

        // then: it should be properly stored in repository as a single record
        val records = statisticsFacade.findRecordsByPhrase(REQUEST, uri)
        assertEquals(1, records.size)
        assertEquals(2, records.get(0).count)
    }

}