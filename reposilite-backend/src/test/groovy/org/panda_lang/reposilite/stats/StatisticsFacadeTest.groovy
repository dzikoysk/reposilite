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

package org.panda_lang.reposilite.stats

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.error.FailureService
import org.panda_lang.reposilite.storage.infrastructure.FileSystemStorageProvider

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals

class StatisticsFacadeTest {

    @TempDir
    protected static Path WORKING_DIRECTORY

    private StatisticsFacade service = new StatisticsFacade(WORKING_DIRECTORY, new FailureService(), FileSystemStorageProvider.of(WORKING_DIRECTORY, "10GB"))

    @Test
    void 'should count unique records' () {
        assertEquals 0, service.loadAggregatedStats().get().countRecords()

        service.record('/record1')
        service.record('/record1')
        assertEquals 1, service.loadAggregatedStats().get().countUniqueRecords()

        service.record('/record2')
        service.record('/record2')
        assertEquals 2, service.loadAggregatedStats().get().countUniqueRecords()
    }

}
