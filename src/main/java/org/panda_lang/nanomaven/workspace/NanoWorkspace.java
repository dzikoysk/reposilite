/*
 * Copyright (c) 2017 Dzikoysk
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

package org.panda_lang.nanomaven.workspace;

import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.util.FilesUtils;

public class NanoWorkspace {


    public void prepare() {
        NanoMaven.getLogger().info("Preparing workspace");
        DefaultWorkspace defaultWorkspace = new DefaultWorkspace();

        // Repositories
        if (!FilesUtils.fileExists("repositories")) {
            defaultWorkspace.generateRepositories();
            NanoMaven.getLogger().info("Default repositories have been created");
        }
        else {
            NanoMaven.getLogger().info("Using an existing repositories");
        }

        // Maven
        if (!FilesUtils.fileExists("maven")) {
            NanoMaven.getLogger().info("Unpacking Maven...");
            defaultWorkspace.generateMaven();
            NanoMaven.getLogger().info("Nested Maven has been unpacked");
        }
        else {
            NanoMaven.getLogger().info("Using an existing nested maven library");
        }

        // Users data
        if (!FilesUtils.fileExists("data/users.pc")) {
            NanoMaven.getLogger().info("Generating users data file...");
            defaultWorkspace.generateUsers();
            NanoMaven.getLogger().info("Empty users data file has been generated");
        }
        else {
            NanoMaven.getLogger().info("Using an existing users data file");
        }

        // Projects data
        if (!FilesUtils.fileExists("data/projects.pc")) {
            NanoMaven.getLogger().info("Generating projects data file...");
            defaultWorkspace.generateProjects();
            NanoMaven.getLogger().info("Empty projects data file has been generated");
        }
        else {
            NanoMaven.getLogger().info("Using an existing projects data file");
        }
    }

}
