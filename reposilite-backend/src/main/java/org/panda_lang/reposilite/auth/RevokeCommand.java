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

import org.panda_lang.reposilite.console.ReposiliteCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.List;

@Command(name = "revoke", description = "Revoke token")
final class RevokeCommand implements ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<alias>", description = "alias of token to revoke")
    private String alias;

    private final TokenService tokenService;

    public RevokeCommand(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean execute(List<String> response) {
        Token token = tokenService.deleteToken(alias);

        if (token == null) {
            response.add("Alias '" + alias + "' not found");
            return false;
        }

        try {
            tokenService.saveTokens();
            response.add("Token for '" + alias + "' has been revoked");
            return true;
        } catch (IOException e) {
            response.add("Cannot remove token due to: " + e);
            response.add("Token has been restored.");
            tokenService.addToken(token);
            return false;
        }
    }

}
