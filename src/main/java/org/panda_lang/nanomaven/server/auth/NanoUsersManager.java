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

import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.workspace.configuration.NanoMavenConfiguration;
import org.panda_lang.nanomaven.workspace.data.users.NanoUserDatabase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NanoUsersManager {

    private final Map<String, NanoUser> users = new HashMap<>();
    private final NanoUserDatabase database;

    public NanoUsersManager() {
        this.database = new NanoUserDatabase(this);
    }

    public void load(NanoMavenConfiguration configuration) {
        this.database.loadUsers();

        for (String admin : configuration.getAdministrators()) {
            NanoUser user = getUser(admin);

            if (user == null) {
                NanoMaven.getLogger().warn("Cannot grant admin permissions [user " + admin + " doesn't exist]");
                continue;
            }

            user.enableAdministrator();
            NanoMaven.getLogger().info("Administrator permissions has been granted to '" + admin + "'");
        }
    }

    public void save() {
        this.database.saveUsers();
    }

    public void addUser(NanoUser user) {
        this.users.put(user.getUsername(), user);
    }

    public NanoUser getUser(String username) {
        return users.get(username);
    }

    public int getAmountOfUsers() {
        return users.size();
    }

    public Collection<NanoUser> getUsers() {
        return users.values();
    }

}
