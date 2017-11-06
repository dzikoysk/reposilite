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

package org.panda_lang.nanomaven.console.commands;

import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.console.NanoCommand;
import org.panda_lang.nanomaven.server.auth.NanoProject;
import org.panda_lang.nanomaven.server.auth.NanoProjectsManager;
import org.panda_lang.nanomaven.server.auth.NanoUser;
import org.panda_lang.nanomaven.server.auth.NanoUsersManager;

public class AddMemberCommand implements NanoCommand {

    private final String projectName;
    private final String username;

    public AddMemberCommand(String projectName, String username) {
        this.projectName = projectName;
        this.username = username;
    }

    @Override
    public void call(NanoMaven nanoMaven) {
        NanoUsersManager usersManager = nanoMaven.getUsersManager();
        NanoProjectsManager projectsManager = nanoMaven.getProjectsManager();

        if (!projectName.contains("/")) {
            NanoMaven.getLogger().warn("GroupId or ArtifactId is not specified");
            return;
        }

        NanoProject nanoProject = projectsManager.getProject(projectName);

        if (nanoProject == null) {
            NanoMaven.getLogger().warn("Project " + projectName + " doesn't exists");
            return;
        }

        NanoUser nanoUser = usersManager.getUser(username);

        if (nanoUser == null) {
            NanoMaven.getLogger().warn("User " + username + " doesn't exists");
            return;
        }

        nanoProject.addUser(nanoUser);
        nanoUser.addProject(nanoProject);

        projectsManager.save();
        NanoMaven.getLogger().info("User " + username + " has been added to the '" + projectName + "' project");
    }

}
