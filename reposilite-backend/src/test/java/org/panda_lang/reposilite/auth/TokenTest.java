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
        assertEquals("alias", token.getToken());

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