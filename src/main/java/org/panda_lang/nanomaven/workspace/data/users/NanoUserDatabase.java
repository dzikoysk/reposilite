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
import org.panda_lang.nanomaven.server.auth.NanoUser;
import org.panda_lang.nanomaven.server.auth.NanoUsersManager;
import org.panda_lang.panda.utilities.configuration.PandaConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.File;
import java.util.Map.Entry;

public class NanoUserDatabase {

    public static final BCryptPasswordEncoder B_CRYPT_PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private static final File USERS_FILE = new File("data/users.pc");
    private final NanoUsersManager usersManager;

    public NanoUserDatabase(NanoUsersManager usersManager) {
        this.usersManager = usersManager;
    }

    public void loadUsers() {
        PandaConfiguration configuration = new PandaConfiguration(USERS_FILE);

        for (Entry<String, Object> entry : configuration.getMap().entrySet()) {
            String username = entry.getKey();
            String password = entry.getValue().toString();

            NanoUser user = new NanoUser(username);
            user.setEncryptedPassword(password);

            usersManager.addUser(user);
        }

        NanoMaven.getLogger().info("Loaded " + usersManager.getAmountOfUsers() + " users");
    }

    public void saveUsers() {
        PandaConfiguration configuration = new PandaConfiguration();

        for (NanoUser user : usersManager.getUsers()) {
            configuration.set(user.getUsername(), user.getEncryptedPassword());
        }

        configuration.save(USERS_FILE);
        NanoMaven.getLogger().info("Saved " + configuration.getMap().size() + " users");
    }

}
