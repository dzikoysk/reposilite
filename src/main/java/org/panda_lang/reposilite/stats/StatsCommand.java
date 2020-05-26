package org.panda_lang.reposilite.stats;

import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.console.NanoCommand;
import org.panda_lang.utilities.commons.console.Effect;

import java.util.Map;
import java.util.Map.Entry;

public final class StatsCommand implements NanoCommand {

    private final int limiter;

    public StatsCommand(int limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean call(Reposilite reposilite) {
        StatsService statsService = reposilite.getStatsService();
        Map<String, Integer> stats = statsService.fetchStats(entry -> entry.getValue() >= limiter);

        Reposilite.getLogger().info("");
        Reposilite.getLogger().info("Statistics: ");
        Reposilite.getLogger().info("  Requests count: " + statsService.countRecords() + " (sum: " + statsService.sumRecords() + ")");
        Reposilite.getLogger().info("  Recorded: " + (stats.isEmpty() ? "[] " : "") + "(list filtered by at least " + Effect.BLACK_BOLD + limiter + Effect.RESET + " recorded requests)");
        int order = 0;

        for (Entry<String, Integer> entry : stats.entrySet()) {
            Reposilite.getLogger().info("    " + (++order) + ". (" + entry.getValue() + ") " + entry.getKey());
        }

        Reposilite.getLogger().info("");
        return true;
    }

}
