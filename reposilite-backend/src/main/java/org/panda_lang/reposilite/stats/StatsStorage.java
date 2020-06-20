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
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.utils.YamlUtils;

import java.io.File;
import java.io.IOException;

public final class StatsStorage {

    private final File statsFile;

    public StatsStorage(String workingDirectory) {
        this.statsFile = new File(workingDirectory, ReposiliteConstants.STATS_FILE_NAME);
    }

    public StatsEntity loadStats() throws IOException {
        if (!statsFile.exists()) {
            Reposilite.getLogger().info("Generating stats data file...");
            FilesUtils.copyResource("/" + ReposiliteConstants.STATS_FILE_NAME, statsFile);
            Reposilite.getLogger().info("Empty stats file has been generated");
        }
        else {
            Reposilite.getLogger().info("Using an existing stats data file");
        }

        StatsEntity statsEntity = YamlUtils.load(statsFile, StatsEntity.class);
        Reposilite.getLogger().info("Records: " + statsEntity.getRecords().size());

        return statsEntity;
    }

    public void saveStats(StatsEntity entity) throws IOException {
        YamlUtils.save(statsFile, entity);
        Reposilite.getLogger().info("Stored records: " + entity.getRecords().size());
    }

}
