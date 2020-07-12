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

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.utilities.commons.collection.Pair;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeygenCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldCreateNewToken() {
        TokenService tokenService = super.reposilite.getTokenService();
        tokenService.createToken("/a", "alias");

        KeygenCommand keygenCommand = new KeygenCommand("/a/b/c", "alias");
        assertTrue(keygenCommand.execute(super.reposilite));

        Token token = reposilite.getTokenService().getToken("alias");
        assertNotNull(token);
        assertEquals("/a/b/c", token.getPath());
        assertEquals("alias", token.getAlias());
        assertNotNull(token.getToken());
    }

    @Test
    void shouldCreateTokenBasedOnQualifier() {
        KeygenCommand keygenCommand = new KeygenCommand("org.panda-lang.reposilite", "reposilite");
        assertTrue(keygenCommand.execute(super.reposilite));
        assertEquals("*/org/panda-lang/reposilite", super.reposilite.getTokenService().getToken("reposilite").getPath());
    }

    @Test
    void shouldFalseIfFileIsNotAvailable() throws IOException {
        super.reposilite.getTokenService().createToken("/", "alias");
        File tokensFile = new File(super.workingDirectory, "tokens.yml");
        executeOnLocked(tokensFile, () -> assertFalse(new KeygenCommand("/a/b/c", "alias").execute(super.reposilite)));
        assertTrue(new KeygenCommand("/a/b/c", "alias").execute(super.reposilite));
    }

}