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

import org.panda_lang.nanomaven.util.DirectoryUtils;
import org.panda_lang.nanomaven.util.FileUtils;
import org.panda_lang.nanomaven.util.ZipUtils;

public class DefaultWorkspace {

    protected DefaultWorkspace() {
    }

    public void generateConfiguration(){
        FileUtils.excludeResource("/nanomaven.yml", "nanomaven.yml");
    }

    public void generateRepositories() {
        DirectoryUtils.createDirectories("repositories/releases", "repositories/snapshots");
    }

    public void generateMaven() {
        ZipUtils.unzipResource("/apache-maven-3.5.0.zip", "maven");
    }

    public void generateUsers() {
        FileUtils.createFile("data/users.pc");
    }

    public void generateProjects() {
        FileUtils.createFile("data/projects.pc");
    }

}
