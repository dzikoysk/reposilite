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
import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.utils.YamlUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public final class TokensStorage {

    private final TokenService tokenService;
    private final File tokensFile;

    public TokensStorage(TokenService tokenService, String workingDirectory) {
        this.tokenService = tokenService;
        this.tokensFile = new File(workingDirectory, ReposiliteConstants.TOKENS_FILE_NAME);
    }

    public void loadTokens() throws IOException {
        if (!tokensFile.exists()) {
            Reposilite.getLogger().info("Generating tokens data file...");
            FilesUtils.copyResource("/" + ReposiliteConstants.TOKENS_FILE_NAME, tokensFile);
            Reposilite.getLogger().info("Empty tokens file has been generated");
        }
        else {
            Reposilite.getLogger().info("Using an existing tokens data file");
        }

        TokenCollection tokenCollection = YamlUtils.load(tokensFile, TokenCollection.class);

        for (Token token : tokenCollection.getTokens()) {
            tokenService.addToken(token);
        }

        Reposilite.getLogger().info("Tokens: " + tokenService.count());
    }

    public void saveTokens() throws IOException {
        TokenCollection tokenCollection = new TokenCollection();
        tokenCollection.setTokens(new ArrayList<>());

        for (Token token : tokenService.getTokens()) {
            tokenCollection.getTokens().add(token);
        }

        YamlUtils.save(tokensFile, tokenCollection);
        Reposilite.getLogger().info("Stored tokens: " + tokenCollection.getTokens().size());
    }

}
