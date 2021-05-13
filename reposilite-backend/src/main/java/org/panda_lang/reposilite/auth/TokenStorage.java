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

import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.storage.StorageProvider;
import org.panda_lang.reposilite.utils.YamlUtils;
import org.panda_lang.utilities.commons.function.Option;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;

public final class TokenStorage {

    private final TokenService tokenService;
    private final Path tokensFile;
    private final StorageProvider storageProvider;

    public TokenStorage(TokenService tokenService, Path workingDirectory, StorageProvider storageProvider) {
        this.tokenService = tokenService;
        this.tokensFile = workingDirectory.resolve(ReposiliteConstants.TOKENS_FILE_NAME);
        this.storageProvider = storageProvider;
    }

    public void loadTokens() throws IOException {
        if (!storageProvider.exists(tokensFile)) {
            Path legacyTokensFile = tokensFile.resolveSibling(tokensFile.getFileName().toString().replace(".dat", ".yml"));

            if (storageProvider.exists(legacyTokensFile)) {
                Result<byte[], ErrorDto> result = storageProvider.getFile(legacyTokensFile);

                if (result.isOk()) {
                    storageProvider.putFile(tokensFile, result.get());
                    tokenService.getLogger().info("Legacy tokens file has been converted to dat file");
                } else {
                    throw new IOException(result.getError().getMessage());
                }
            } else {
                tokenService.getLogger().info("Generating tokens data file...");
                storageProvider.putFile(tokensFile, "!!org.panda_lang.reposilite.auth.TokenCollection\n\"tokens\": []".getBytes(StandardCharsets.UTF_8));
                tokenService.getLogger().info("Empty tokens file has been generated");
            }
        } else {
            tokenService.getLogger().info("Using an existing tokens data file");
        }

        TokenCollection tokenCollection = YamlUtils.load(storageProvider, tokensFile, TokenCollection.class);

        for (Token token : tokenCollection.getTokens()) {
            // Update missing default permissions of old tokens
            // ~ https://github.com/dzikoysk/reposilite/issues/233
            token.setPermissions(Option.of(token.getPermissions()).orElseGet(Permission.toString(Permission.getDefaultPermissions())));
            tokenService.addToken(token);
        }

        tokenService.getLogger().info("Tokens: " + tokenService.count());
    }

    public void saveTokens() throws IOException {
        TokenCollection tokenCollection = new TokenCollection();
        tokenCollection.setTokens(new ArrayList<>());

        for (Token token : tokenService.getTokens()) {
            tokenCollection.getTokens().add(token);
        }

        YamlUtils.save(storageProvider, tokenCollection, tokensFile);
        tokenService.getLogger().info("Stored tokens: " + tokenCollection.getTokens().size());
    }

}
