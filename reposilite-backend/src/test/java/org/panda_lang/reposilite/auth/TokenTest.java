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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenTest {

    @Test
    void isWildcard() {
        Token standard = new Token("/a/b/c", "standard", "secret");
        assertFalse(standard.isWildcard());

        Token wildcard = new Token("*/a/b/c", "wildcard", "secret");
        assertTrue(wildcard.isWildcard());
    }

    @Test
    void hasMultiaccess() {
        Token multiaccess = new Token("/", "multiaccess", "secret");
        assertTrue(multiaccess.hasMultiaccess());

        Token multiaccessWildcard = new Token("*", "multiaccess", "secret");
        assertTrue(multiaccessWildcard.hasMultiaccess());
    }

    @Test
    void token() {
        Token token = new Token("path", "alias", "giga_secret");
        assertEquals("giga_secret", token.getToken());

        Token deserializedToken = new Token();
        deserializedToken.setToken("secret");
        assertEquals("secret", deserializedToken.getToken());
    }

    @Test
    void alias() {
        Token token = new Token("path", "alias", "secret");
        assertEquals("alias", token.getAlias());

        Token deserializedToken = new Token();
        deserializedToken.setAlias("alias");
        assertEquals("alias", deserializedToken.getAlias());
    }

    @Test
    void path() {
        Token token = new Token("path", "alias", "secret");
        assertEquals("path", token.getPath());

        Token deserializedToken = new Token();
        deserializedToken.setPath("/a/b/c");
        assertEquals("/a/b/c", deserializedToken.getPath());
    }

}