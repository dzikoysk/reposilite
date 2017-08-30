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
import org.panda_lang.nanomaven.server.auth.NanoUser;
import org.panda_lang.nanomaven.server.auth.NanoUsersManager;
import org.panda_lang.nanomaven.workspace.data.users.NanoUserDatabase;

public class UserCommand implements NanoCommand {

    private final String username;
    private final String password;

    public UserCommand(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void call(NanoMaven nanoMaven) {
        NanoUsersManager usersManager = nanoMaven.getUsersManager();
        NanoUser nanoUser = usersManager.getUser(username);

        if (nanoUser != null) {
            NanoMaven.getLogger().warn("User " + username + " exists");
            return;
        }

        NanoMaven.getLogger().info("Creating user...");

        NanoUser user = new NanoUser(username);
        usersManager.addUser(user);

        String encodedPassword = NanoUserDatabase.B_CRYPT_PASSWORD_ENCODER.encode(password);
        user.setEncryptedPassword(encodedPassword);

        usersManager.save();
        NanoMaven.getLogger().info("User " + username + " has been created");
    }

}
