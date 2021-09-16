package com.reposilite.statistics.specification

import com.reposilite.ReposiliteSpecification
import kong.unirest.Unirest.get
import kotlinx.coroutines.runBlocking

internal abstract class StatisticsIntegrationSpecification : ReposiliteSpecification() {

    fun useRecordedRecord(uri: String): String = runBlocking {
        get("$base$uri").asEmpty()
        reposilite.statisticsFacade.saveRecordsBulk()
        uri
    }

}