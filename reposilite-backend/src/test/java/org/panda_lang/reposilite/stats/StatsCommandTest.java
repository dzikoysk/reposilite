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

package org.panda_lang.reposilite.stats;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.reposilite.ReposiliteWriter;

import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturnTrue() {
        assertTrue(callDefaultStatusCommand());
    }

    @Test
    void shouldDisplayRecord() throws InterruptedException {
        StatsService statsService = super.reposilite.getStatsService();
        statsService.record("/record");
        callDefaultStatusCommand();

        Thread.sleep(10);
        assertTrue(ReposiliteWriter.contains("/record"));
    }

    @Test
    void shouldFilterExtensions() throws InterruptedException {
        StatsService statsService = super.reposilite.getStatsService();

        String[] records = {
                "/record.md5",
                "/record.sha1",
                "/record.pom",
                "/js/app.js",
        };

        Arrays.stream(records).forEach(statsService::record);
        StatsCommand statsCommand = new StatsCommand(2);
        statsCommand.execute(super.reposilite);

        Thread.sleep(10);
        Arrays.stream(records).forEach(record -> assertFalse(ReposiliteWriter.contains(record)));
    }

    @Test
    void shouldLimitEntries() {
        StatsService statsService = super.reposilite.getStatsService();

        IntStream.range(0, 10).forEach(i -> {
            statsService.record("/" + i);
            statsService.record("/" + i);
        });
        IntStream.range(10, 20).forEach(i -> {
            statsService.record("/" + i);
        });

        StatsCommand statsCommand = new StatsCommand(2);
        statsCommand.execute(super.reposilite);

        IntStream.range(0, 10).forEach(i -> assertTrue(ReposiliteWriter.contains("/" + i)));
        IntStream.range(10, 20).forEach(i -> assertFalse(ReposiliteWriter.contains("/" + i)));
    }

    private boolean callDefaultStatusCommand() {
        return new StatsCommand(-1, "").execute(super.reposilite);
    }

}