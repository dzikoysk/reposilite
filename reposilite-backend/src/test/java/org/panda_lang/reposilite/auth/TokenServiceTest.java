package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.utilities.commons.collection.Pair;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {

    @TempDir
    protected File workingDirectory;
    protected TokenService tokenService;

    @BeforeEach
    void prepare() {
        this.tokenService = new TokenService(workingDirectory.getAbsolutePath());
    }

    @Test
    void shouldSaveAndLoad() throws IOException {
        TokenService tempService = new TokenService(workingDirectory.getAbsolutePath());
        tempService.createToken("path", "alias");
        tempService.save();

        tokenService.load(); // uses the same file
        Token token = tokenService.getToken("alias");
        assertEquals("path", token.getPath());
    }

    @Test
    void createToken() {
        Pair<String, Token> result = tokenService.createToken("path", "alias");
        assertNotNull(tokenService.getToken("alias"));

        Token token = result.getValue();
        assertEquals("path", token.getPath());
        assertEquals("alias", token.getAlias());
        assertTrue(TokenService.B_CRYPT_TOKENS_ENCODER.matches(result.getKey(), token.getToken()));

        Pair<String, Token> customResult = tokenService.createToken("custom_path", "custom_alias", "secret");
        assertNotNull(tokenService.getToken("custom_alias"));

        Token customToken = customResult.getValue();
        assertEquals("custom_path", customToken.getPath());
        assertEquals("custom_alias", customToken.getAlias());
        assertTrue(TokenService.B_CRYPT_TOKENS_ENCODER.matches("secret", customToken.getToken()));
    }

    @Test
    void addToken() {
        Token token = new Token("path", "alias", "secret");
        tokenService.addToken(token);
        assertEquals(token, tokenService.getToken("alias"));
    }

    @Test
    void deleteToken() {
        assertNull(tokenService.deleteToken("random"));

        tokenService.createToken("path", "alias", "token");
        Token token = tokenService.deleteToken("alias");
        assertNotNull(token);
        assertEquals("alias", token.getAlias());

        assertNull(tokenService.getToken("alias"));
    }

    @Test
    void getToken() {
        assertNull(tokenService.getToken("random"));
        tokenService.createToken("path", "alias");
        assertNotNull(tokenService.getToken("alias"));
    }

    @Test
    void count() {
        assertEquals(0, tokenService.count());

        tokenService.createToken("a", "a");
        tokenService.createToken("b", "b");
        assertEquals(2, tokenService.count());

        tokenService.deleteToken("a");
        assertEquals(1, tokenService.count());
    }

    @Test
    void getTokens() {
        assertIterableEquals(Collections.emptyList(), tokenService.getTokens());

        Pair<String, Token> token = tokenService.createToken("path", "alias");
        assertIterableEquals(Collections.singletonList(token.getValue()), tokenService.getTokens());
    }

}