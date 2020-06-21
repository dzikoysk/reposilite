package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevokeCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldRevokeExistingToken() {
        TokenService tokenService = reposilite.getTokenService();
        tokenService.addToken(new Token("path", "alias", "secret"));

        assertTrue(new RevokeCommand("alias").execute(reposilite));
        assertNull(tokenService.getToken("alias"));
    }

    @Test
    void shouldFalseIfTokenDoesNotExist() {
        assertFalse(new RevokeCommand("unknown_token").execute(super.reposilite));
    }

    @Test
    void shouldFalseIfFileIsNotAvailable() throws Exception {
        super.reposilite.getTokenService().addToken(new Token("path", "alias", "secret"));
        File tokensFile = new File(super.workingDirectory, "tokens.yml");
        executeOnLocked(tokensFile, () -> assertFalse(new RevokeCommand("alias").execute(reposilite)));
        assertTrue(new RevokeCommand("alias").execute(reposilite));
    }

}