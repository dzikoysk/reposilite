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

import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.storage.StorageProvider;
import org.panda_lang.reposilite.utils.YamlUtils;
import org.panda_lang.utilities.commons.function.Result;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class StatsStorage {

    private final Path statsFile;
    private final FailureService failureService;
    private final StorageProvider storageProvider;

    public StatsStorage(Path workingDirectory, FailureService failureService, StorageProvider storageProvider) {
        this.statsFile = workingDirectory.resolve(ReposiliteConstants.STATS_FILE_NAME);
        this.failureService = failureService;
        this.storageProvider = storageProvider;
    }

    CompletableFuture<StatsEntity> loadStoredStats() {
        return loadStoredStats(new CompletableFuture<>());
    }

    CompletableFuture<StatsEntity> loadStoredStats(CompletableFuture<StatsEntity> loadTask) {
        try {
            if (!storageProvider.exists(statsFile)) {
                Path legacyStatsFile = statsFile.resolveSibling(statsFile.getFileName().toString().replace(".dat", ".yml"));

                if (storageProvider.exists(legacyStatsFile)) {
                    Result<byte[], ErrorDto> result = storageProvider.getFile(legacyStatsFile);

                    if (result.isOk()) {
                        storageProvider.putFile(statsFile, result.get());
                        failureService.getLogger().info("Legacy stats file has been converted to dat file");
                    } else {
                        failureService.throwException("Cannot load legacy stats file: " + result.getError().getMessage(), new RuntimeException());
                    }
                }
                else {
                    failureService.getLogger().info("Generating stats data file...");
                    storageProvider.putFile(this.statsFile, "!!org.panda_lang.reposilite.stats.StatsEntity\n\"records\": {}".getBytes(StandardCharsets.UTF_8));
                    failureService.getLogger().info("Empty stats file has been generated");
                }
            }

            loadTask.complete(YamlUtils.load(storageProvider, statsFile, StatsEntity.class));
        } catch (Exception exception) {
            failureService.throwException("Cannot load stats file", exception);
            loadTask.cancel(true);
        }

        return loadTask;
    }

    public void saveStats(StatsEntity entity) {
        YamlUtils.save(storageProvider, entity, statsFile);
        failureService.getLogger().info("Stored records: " + entity.getRecords().size());
    }

}
