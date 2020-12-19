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

import java.util.List;

@Command(name = "tokens", description = "List all generated tokens")
final class TokensCommand implements ReposiliteCommand {

    private final TokenService tokenService;

    public TokensCommand(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean execute(List<String> response) {
        response.add("Tokens (" + tokenService.count() + ")");

        for (Token token : tokenService.getTokens()) {
            response.add(token.getPath() + " as " + token.getAlias() + " with '" + token.getPermissions() + "' permissions");
        }

        return true;
    }

}
