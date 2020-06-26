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
    void shouldDisplayRecord() {
        StatsService statsService = super.reposilite.getStatsService();
        statsService.record("/record");
        callDefaultStatusCommand();
        assertTrue(ReposiliteWriter.contains("/record"));
    }

    @Test
    void shouldFilterExtensions() {
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