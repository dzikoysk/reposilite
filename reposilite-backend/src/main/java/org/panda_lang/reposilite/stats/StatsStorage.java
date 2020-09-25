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
import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.utils.YamlUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class StatsStorage {

    private static final long RETRY_TIME = 1000L;

    private final File statsFile;
    private final FailureService failureService;
    private final ExecutorService ioService;
    private final ScheduledExecutorService retryService;

    public StatsStorage(String workingDirectory, FailureService failureService, ExecutorService ioService, ScheduledExecutorService retryService) {
        this.statsFile = new File(workingDirectory, ReposiliteConstants.STATS_FILE_NAME);
        this.failureService = failureService;
        this.ioService = ioService;
        this.retryService = retryService;
    }

    CompletableFuture<StatsEntity> loadStoredStats() {
        return loadStoredStats(new CompletableFuture<>());
    }

    CompletableFuture<StatsEntity> loadStoredStats(CompletableFuture<StatsEntity> loadTask) {
        ioService.submit(() -> {
            try {
                File lockFile = new File(statsFile.getAbsolutePath() + ".lock");

                if (lockFile.exists()) {
                    return retryService.schedule(() -> {
                        ioService.submit(() -> {
                            loadStoredStats(loadTask);
                        });
                    }, RETRY_TIME, TimeUnit.MILLISECONDS);
                }

                if (!statsFile.exists()) {
                    File legacyStatsFile = new File(statsFile.getAbsolutePath().replace(".dat", ".yml"));

                    if (legacyStatsFile.exists()) {
                        Files.move(legacyStatsFile.toPath(), statsFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                        Reposilite.getLogger().info("Legacy stats file has been converted to dat file");
                    }
                    else {
                        Reposilite.getLogger().info("Generating stats data file...");
                        FilesUtils.copyResource("/" + ReposiliteConstants.STATS_FILE_NAME, statsFile);
                        Reposilite.getLogger().info("Empty stats file has been generated");
                    }
                }

                return loadTask.complete(YamlUtils.load(statsFile, StatsEntity.class));
            } catch (Exception exception) {
                failureService.throwException("Cannot load stats file", exception);
                return loadTask.cancel(true);
            }
        });

        return loadTask;
    }

    public void saveStats(StatsEntity entity) throws IOException {
        YamlUtils.save(statsFile, entity);
        Reposilite.getLogger().info("Stored records: " + entity.getRecords().size());
    }

}
