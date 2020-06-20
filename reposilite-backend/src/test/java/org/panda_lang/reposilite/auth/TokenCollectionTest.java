package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenCollectionTest {

    @Test
    void testTokens() {
        Token token = new Token();
        TokenCollection tokenCollection = new TokenCollection();
        tokenCollection.setTokens(Arrays.asList(token, token));

        assertEquals(Arrays.asList(token, token), tokenCollection.getTokens());
    }

}