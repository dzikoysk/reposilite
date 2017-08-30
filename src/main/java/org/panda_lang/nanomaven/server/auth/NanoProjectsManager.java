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

package org.panda_lang.nanomaven.server.auth;

import org.panda_lang.nanomaven.workspace.data.users.NanoProjectsDatabase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NanoProjectsManager {

    private final Map<String, NanoProject> projects = new HashMap<>();
    private final NanoProjectsDatabase database;

    public NanoProjectsManager(NanoUsersManager usersManager) {
        this.database = new NanoProjectsDatabase(this, usersManager);
    }

    public void load() {
        this.database.loadProjects();
    }

    public void save() {
        this.database.saveProjects();
    }

    public void addProject(NanoProject project) {
        this.projects.put(project.getProjectName(), project);
    }

    public NanoProject getProject(String projectName) {
        return projects.get(projectName);
    }

    public int getAmountOfProjects() {
        return projects.size();
    }

    public Collection<NanoProject> getProjects() {
        return projects.values();
    }

}
