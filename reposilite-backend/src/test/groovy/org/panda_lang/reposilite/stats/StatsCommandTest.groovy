/*
 * Copyright (c) 2020 Dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.panda_lang.reposilite.stats

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteTestSpecification
import org.panda_lang.reposilite.ReposiliteWriter

import java.util.stream.IntStream

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class StatsCommandTest extends ReposiliteTestSpecification {

    @Test
    void 'should return true if succeed' () {
        assertTrue callDefaultStatusCommand()
    }

    @Test
    void 'should display record' () throws InterruptedException {
        def statsService = super.reposilite.getStatsService()
        statsService.record('/record')
        callDefaultStatusCommand()

        Thread.sleep(100) // make sure that tinylog service had a chance to store log
        assertTrue ReposiliteWriter.contains('/record')
    }

    @Test
    void 'should filter extensions' () throws InterruptedException {
        def statsService = super.reposilite.getStatsService()

        String[] records = [
                '/record.md5',
                '/record.sha1',
                '/record.pom',
                '/js/app.js'
        ]
        
        Arrays.stream(records).forEach({ record -> statsService.record(record) })
        def statsCommand = new StatsCommand(2)
        statsCommand.execute(super.reposilite)

        Thread.sleep(100) // make sure that tinylog service had a chance to store log
        Arrays.stream(records).forEach({ record -> assertFalse ReposiliteWriter.contains(record) })
    }

    @Test
    void 'should limit entries' () {
        def statsService = super.reposilite.getStatsService()

        IntStream.range(0, 10).forEach({ i ->
            statsService.record('/' + i)
            statsService.record('/' + i)
        })
        IntStream.range(10, 20).forEach({ i ->
            statsService.record('/' + i)
        })

        def statsCommand = new StatsCommand(2)
        statsCommand.execute(super.reposilite)

        Thread.sleep(100) // make sure that tinylog service had a chance to store log
        IntStream.range(0, 10).forEach({ i -> assertTrue ReposiliteWriter.contains('/' + i) })
        IntStream.range(10, 20).forEach({ i -> assertFalse ReposiliteWriter.contains('/' + i) })
    }

    private boolean callDefaultStatusCommand() {
        return new StatsCommand(-1, '').execute(super.reposilite)
    }

}