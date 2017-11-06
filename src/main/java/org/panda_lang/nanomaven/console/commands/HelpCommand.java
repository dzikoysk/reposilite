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
import org.panda_lang.nanomaven.NanoMavenConstants;
import org.panda_lang.nanomaven.console.NanoCommand;

public class HelpCommand implements NanoCommand {

    @Override
    public void call(NanoMaven nanoMaven) {
        NanoMaven.getLogger().info("");
        NanoMaven.getLogger().info("NanoMaven " + NanoMavenConstants.VERSION + " Commands:");
        NanoMaven.getLogger().info("  help - List available commands");
        NanoMaven.getLogger().info("  users - List all registered users");
        NanoMaven.getLogger().info("  projects - List all added projects");
        NanoMaven.getLogger().info("  add-user <username> <password> - Add user");
        NanoMaven.getLogger().info("  add-project <repository>.<groupId>/<artifactId> - Add project extra data");
        NanoMaven.getLogger().info("  add-member <repository>.<groupId>/<artifactId> <username> - Add user to the specified project");
        NanoMaven.getLogger().info("  reinstall-artifacts (rs) - Reinstall all artifacts");
        NanoMaven.getLogger().info("");
    }

}
