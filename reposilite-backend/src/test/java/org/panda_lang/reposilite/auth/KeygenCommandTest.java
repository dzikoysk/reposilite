package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

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
    void shouldFalseIfFileIsNotAvailable() throws IOException {
        super.reposilite.getTokenService().createToken("/", "alias");
        File tokensFile = new File(super.workingDirectory, "tokens.yml");
        executeOnLocked(tokensFile, () -> assertFalse(new KeygenCommand("/a/b/c", "alias").execute(super.reposilite)));
        assertTrue(new KeygenCommand("/a/b/c", "alias").execute(super.reposilite));
    }

}