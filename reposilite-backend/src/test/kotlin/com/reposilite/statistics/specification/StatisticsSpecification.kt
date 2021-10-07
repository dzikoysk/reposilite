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

package com.reposilite.statistics.specification

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.api.RecordType
import com.reposilite.statistics.infrastructure.InMemoryStatisticsRepository

internal open class StatisticsSpecification {

    private val logger = InMemoryLogger()
    protected val statisticsFacade = StatisticsFacade(logger, InMemoryStatisticsRepository())

    protected fun useRecordedIdentifier(type: RecordType, identifier: String, times: Int = 1): Pair<RecordType, String> {
        repeat(times) {
            increaseAndSave(type, identifier)
        }

        return Pair(type, identifier)
    }

    protected fun increaseAndSave(type: RecordType, identifier: String) {
        statisticsFacade.increaseRecord(type, identifier)
        statisticsFacade.saveRecordsBulk()
    }

}