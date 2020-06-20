package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenListCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldListAllTokens() {
        TokenService tokenService = super.reposilite.getTokenService();
        tokenService.createToken("/a", "a");
        tokenService.createToken("/b", "b");

        TokenListCommand tokenListCommand = new TokenListCommand();
        assertTrue(tokenListCommand.call(super.reposilite));
    }

}