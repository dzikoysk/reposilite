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

import org.panda_lang.reposilite.error.FailureService;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public final class StatsService {

    private final StatsEntity instanceStats = new StatsEntity();
    private final StatsStorage statsStorage;

    public StatsService(String workingDirectory, FailureService failureService, ExecutorService ioService, ScheduledExecutorService retryService) {
        this.statsStorage = new StatsStorage(workingDirectory, failureService, ioService, retryService);
    }

    public void record(String uri) {
        instanceStats.getRecords().compute(uri, (key, count) -> (count == null) ? 1 : count + 1);
    }

    public void saveStats() throws IOException, ExecutionException, InterruptedException {
        statsStorage.saveStats(loadAggregatedStats().get().getAggregatedStatsEntity());
    }

    public CompletableFuture<AggregatedStats> loadAggregatedStats() {
        return statsStorage.loadStoredStats().thenApply(aggregatedStats -> {
            instanceStats.getRecords().forEach((key, value) -> {
                aggregatedStats.getRecords().merge(key, value, Integer::sum);
            });

            return new AggregatedStats(aggregatedStats);
        });
    }

}
