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

package org.panda_lang.reposilite.auth;

import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.console.ReposiliteCommand;
import org.panda_lang.utilities.commons.collection.Pair;

import java.io.IOException;

public final class KeygenCommand implements ReposiliteCommand {

    private final String path;
    private final String alias;

    public KeygenCommand(String path, String alias) {
        this.path = path;
        this.alias = alias;
    }

    @Override
    public boolean execute(Reposilite reposilite) {
        TokenService tokenService = reposilite.getTokenService();
        Token previousToken = tokenService.getToken(alias);

        try {
            Pair<String, Token> token = tokenService.createToken(path, alias);
            Reposilite.getLogger().info("Generated new access token for " + alias + " (" + path + ")");
            Reposilite.getLogger().info(token.getKey());
            tokenService.save();
            return true;
        } catch (IOException e) {
            Reposilite.getLogger().info("Cannot generate token due to: " + e.getMessage());

            if (previousToken != null) {
                tokenService.addToken(previousToken);
                Reposilite.getLogger().info("The former token has been restored.");
            }

            return false;
        }
    }

}
