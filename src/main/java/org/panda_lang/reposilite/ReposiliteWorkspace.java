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

package org.panda_lang.reposilite;

import org.panda_lang.reposilite.utils.FilesUtils;

import java.io.File;

final class ReposiliteWorkspace {

    void prepare() {
        // Configuration
        if (!FilesUtils.exists(ReposiliteConstants.CONFIGURATION_FILE_NAME)) {
            Reposilite.getLogger().info("Generating default configuration file.");
            FilesUtils.copyResource("/reposilite.yml", ReposiliteConstants.CONFIGURATION_FILE_NAME);
        }
        else {
            Reposilite.getLogger().info("Using an existing configuration file");
        }

        // Repositories
        if (!FilesUtils.exists("repositories")) {
            createDirectories("repositories/releases", "repositories/snapshots");
            Reposilite.getLogger().info("Default repositories have been created");
        }
        else {
            Reposilite.getLogger().info("Using an existing repositories directory");
        }

        // Tokens data
        if (!FilesUtils.exists(ReposiliteConstants.TOKENS_FILE_NAME)) {
            Reposilite.getLogger().info("Generating tokens data file...");
            FilesUtils.copyResource("/tokens.yml", ReposiliteConstants.TOKENS_FILE_NAME);
            Reposilite.getLogger().info("Empty tokens file has been generated");
        }
        else {
            Reposilite.getLogger().info("Using an existing tokens data file");
        }

        Reposilite.getLogger().info("");
    }

    private boolean createDirectories(String... dirs) {
        for (String dir : dirs) {
            File directory = new File(dir);

            if (directory.exists()) {
                continue;
            }

            if (!directory.mkdirs()) {
                return false;
            }
        }

        return true;
    }

}
