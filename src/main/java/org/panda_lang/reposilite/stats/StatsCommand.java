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

import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.console.NanoCommand;
import org.panda_lang.utilities.commons.console.Effect;

import java.util.Map;
import java.util.Map.Entry;

public final class StatsCommand implements NanoCommand {

    private final String pattern;
    private Long limiter;

    public StatsCommand(long limiter, String pattern) {
        this.limiter = limiter;
        this.pattern = pattern;
    }

    public StatsCommand(long limiter) {
        this(limiter, "");
    }

    public StatsCommand(String pattern) {
        this(0, pattern);
    }

    @Override
    public boolean call(Reposilite reposilite) {
        StatsService statsService = reposilite.getStatsService();
        int count = statsService.countRecords();
        int sum = statsService.sumRecords();

        if (limiter == -1) {
            double avg = sum / (double) count;
            limiter = Math.round(1.5 * avg);
        }

        Map<String, Integer> stats = statsService.fetchStats(entry -> entry.getValue() >= limiter && entry.getKey().contains(pattern));
        int order = 0;

        Reposilite.getLogger().info("");
        Reposilite.getLogger().info("Statistics: ");
        Reposilite.getLogger().info("  Requests count: " + count + " (sum: " + sum + ")");
        Reposilite.getLogger().info("  Recorded: " + (stats.isEmpty() ? "[] " : "") +" (limiter: " + highlight(limiter), ", pattern: '" + highlight(pattern) + "')");

        for (Entry<String, Integer> entry : stats.entrySet()) {
            Reposilite.getLogger().info("    " + (++order) + ". (" + entry.getValue() + ") " + entry.getKey());
        }

        Reposilite.getLogger().info("");
        return true;
    }

    private String highlight(Object value) {
        return Effect.BLACK_BOLD + value.toString() + Effect.RESET;
    }

}
