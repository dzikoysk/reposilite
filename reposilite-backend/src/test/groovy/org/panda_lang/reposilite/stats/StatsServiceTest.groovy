/*
 * Copyright (c) 2020 Dzikoysk
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

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
class StatsServiceTest {

    private StatsService STATS_SERVICE = new StatsService('')

    @Test
    void 'should count records' () {
        assertEquals 0, STATS_SERVICE.countRecords()

        STATS_SERVICE.record('/record')
        assertEquals 1, STATS_SERVICE.countRecords()
    }

    @Test
    void 'should count unique records' () {
        assertEquals 0, STATS_SERVICE.countRecords()

        STATS_SERVICE.record('/record1')
        STATS_SERVICE.record('/record1')
        assertEquals 1, STATS_SERVICE.countUniqueRecords()

        STATS_SERVICE.record('/record2')
        STATS_SERVICE.record('/record2')
        assertEquals 2, STATS_SERVICE.countUniqueRecords()
    }

}
