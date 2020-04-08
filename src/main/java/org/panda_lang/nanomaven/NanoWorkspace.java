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

package org.panda_lang.nanomaven;

import org.panda_lang.nanomaven.utils.DirectoryUtils;
import org.panda_lang.nanomaven.utils.FilesUtils;
import org.panda_lang.nanomaven.utils.ZipUtils;

public class NanoWorkspace {

    public void prepare() {
        NanoMaven.getLogger().info("Preparing workspace");

        if (!FilesUtils.fileExists(NanoConstants.CONFIGURATION_FILE_NAME)) {
            NanoMaven.getLogger().info("Generating default configuration file.");
            FilesUtils.copyResource("/nanomaven.yml", NanoConstants.CONFIGURATION_FILE_NAME);
        }
        else {
            NanoMaven.getLogger().info("Using an existing configuration file");
        }

        // Repositories
        if (!FilesUtils.fileExists("repositories")) {
            DirectoryUtils.createDirectories("repositories/releases", "repositories/snapshots");
            NanoMaven.getLogger().info("Default repositories have been created");
        }
        else {
            NanoMaven.getLogger().info("Using an existing repositories");
        }

        // Maven
        if (!FilesUtils.fileExists("maven")) {
            NanoMaven.getLogger().info("Unpacking Maven...");
            ZipUtils.unzipResource("/apache-maven-3.5.0.zip", "maven");
            NanoMaven.getLogger().info("Nested Maven has been unpacked");
        }
        else {
            NanoMaven.getLogger().info("Using an existing nested maven library");
        }

        // Tokens data
        if (!FilesUtils.fileExists(NanoConstants.TOKENS_FILE_NAME)) {
            NanoMaven.getLogger().info("Generating tokens data file...");
            FilesUtils.copyResource("/tokens.yml", NanoConstants.TOKENS_FILE_NAME);
            NanoMaven.getLogger().info("Empty tokens file has been generated");
        }
        else {
            NanoMaven.getLogger().info("Using an existing tokens data file");
        }
    }

}
