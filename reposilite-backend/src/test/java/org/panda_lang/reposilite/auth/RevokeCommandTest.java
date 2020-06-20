package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevokeCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldRevokeExistingToken() {
        TokenService tokenService = reposilite.getTokenService();
        tokenService.addToken(new Token("path", "alias", "secret"));

        RevokeCommand revokeCommand = new RevokeCommand("alias");
        assertTrue(revokeCommand.call(reposilite));
        assertNull(tokenService.getToken("alias"));
    }

}