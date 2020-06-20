package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeygenCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldCreateNewToken() throws IOException {
        KeygenCommand keygenCommand = new KeygenCommand("/a/b/c", "alias");
        assertTrue(keygenCommand.call(reposilite));

        TokenService tokenService = reposilite.getTokenService();
        Token token = reposilite.getTokenService().getToken("alias");

        assertNotNull(token);
        assertEquals("/a/b/c", token.getPath());
        assertEquals("alias", token.getAlias());
        assertNotNull(token.getToken());

        tokenService.deleteToken("alias");
        tokenService.save();
    }

}