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

package org.panda_lang.nanomaven.workspace.data.users;

import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.server.auth.NanoProject;
import org.panda_lang.nanomaven.server.auth.NanoProjectsManager;
import org.panda_lang.nanomaven.server.auth.NanoUser;
import org.panda_lang.nanomaven.server.auth.NanoUsersManager;
import org.panda_lang.panda.utilities.configuration.PandaConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

public class NanoProjectsDatabase {

    private static final File PROJECTS_FILE = new File("data/projects.pc");
    private final NanoProjectsManager projectsManager;
    private final NanoUsersManager usersManager;

    public NanoProjectsDatabase(NanoProjectsManager projectsManager, NanoUsersManager usersManager) {
        this.projectsManager = projectsManager;
        this.usersManager = usersManager;
    }

    public void loadProjects() {
        PandaConfiguration configuration = new PandaConfiguration(PROJECTS_FILE);

        for (Entry<String, Object> entry : configuration.getMap().entrySet()) {
            String projectName = entry.getKey();

            NanoProject project = new NanoProject(projectName);
            List<String> users = configuration.getStringList(projectName);

            for (String username : users) {
                NanoUser user = usersManager.getUser(username);

                if (user == null) {
                    NanoMaven.getLogger().warn("User " + username + " not found [project: " + projectName + "]");
                    continue;
                }

                user.addProject(project);
                project.addUser(user);
            }

            projectsManager.addProject(project);
        }

        NanoMaven.getLogger().info("Loaded " + projectsManager.getAmountOfProjects() + " projects");
    }

    public void saveProjects() {
        PandaConfiguration configuration = new PandaConfiguration();

        for (NanoProject project : projectsManager.getProjects()) {
            Collection<NanoUser> nanoUsers = project.getUsers();
            List<String> users = new ArrayList<>(nanoUsers.size());

            for (NanoUser user : project.getUsers()) {
                users.add(user.getUsername());
            }

            configuration.set(project.getProjectName(), users);
        }

        configuration.save(PROJECTS_FILE);
        NanoMaven.getLogger().info("Saved " + configuration.getMap().size() + " projects");
    }

}
