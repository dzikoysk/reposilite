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

package org.panda_lang.reposilite.console;

import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteConstants;

final class HelpCommand implements NanoCommand {

    @Override
    public boolean call(Reposilite reposilite) {
        Reposilite.getLogger().info("");
        Reposilite.getLogger().info("Reposilite " + ReposiliteConstants.VERSION + " Commands:");
        Reposilite.getLogger().info("  help - List available commands");
        Reposilite.getLogger().info("  status - Display summary status of app health");
        Reposilite.getLogger().info("  stats [<limiter> = 2] - Display collected metrics and (optional) filter them using the given limiter");
        Reposilite.getLogger().info("  tokens - List all generated tokens");
        Reposilite.getLogger().info("  keygen <path> <alias> - Generate a new access token for the given path");
        Reposilite.getLogger().info("  revoke <alias> - Revoke token");
        Reposilite.getLogger().info("  purge - Clear cache");
        Reposilite.getLogger().info("  stop - Shutdown server");
        Reposilite.getLogger().info("");

        return true;
    }

}
