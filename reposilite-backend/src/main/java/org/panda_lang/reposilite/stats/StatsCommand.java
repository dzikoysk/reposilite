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

import org.panda_lang.reposilite.console.ReposiliteCommand;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.console.Effect;
import org.panda_lang.utilities.commons.function.Option;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

@Command(name = "stats", description = "Display collected metrics")
final class StatsCommand implements ReposiliteCommand {

    private static final int DEFAULT_TOP_SIZE = 20;

    @Parameters(index = "0", paramLabel = "[<filter>]", description = "accepts string as pattern and int as limiter", defaultValue = "-1")
    private String filter;

    private final StatsService statsService;

    StatsCommand(StatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public boolean execute(List<String> response) {
        try {
            loadAndProcessStats(response).get();
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private CompletableFuture<Void> loadAndProcessStats(List<String> response) {
        return statsService.loadAggregatedStats().thenAccept(aggregatedStats -> {
            response.add("Statistics: ");
            response.add("  Unique requests: " + aggregatedStats.countUniqueRecords() + " (count: " + aggregatedStats.countRecords() + ")");

            int limiter = Option.attempt(NumberFormatException.class, () -> Integer.parseInt(filter)).orElseGet(0);
            String pattern = limiter != 0 ? StringUtils.EMPTY : filter;

            Map<String, Integer> stats = aggregatedStats.fetchStats(
                    (uri, counts) -> counts >= limiter,
                    (uri, counts) -> uri.contains(pattern),
                    (uri, counts) -> !uri.endsWith(".md5"),
                    (uri, counts) -> !uri.endsWith(".sha1"),
                    (uri, counts) -> !uri.endsWith(".pom"),
                    (uri, counts) -> !uri.endsWith("/js/app.js")
            );

            response.add("  Recorded: " + (stats.isEmpty() ? "[] " : "") +" (limiter: " + highlight(limiter) + ", pattern: '" + highlight(pattern) + "')");
            int order = 0;

            for (Entry<String, Integer> entry : stats.entrySet()) {
                response.add("    " + (++order) + ". (" + entry.getValue() + ") " + entry.getKey());

                if (limiter == -1 && order == DEFAULT_TOP_SIZE) {
                    break;
                }
            }
        });
    }

    private String highlight(Object value) {
        return Effect.BLACK_BOLD + value.toString() + Effect.RESET;
    }

}
