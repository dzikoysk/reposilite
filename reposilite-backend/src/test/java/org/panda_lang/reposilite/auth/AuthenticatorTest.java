package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.utilities.commons.collection.Maps;

import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatorTest {

    private static final TokenService TOKEN_SERVICE = new TokenService("");
    private static final Token AUTH_TOKEN = new Token("/auth/test", "alias", TokenService.B_CRYPT_TOKENS_ENCODER.encode("secret"));
    private static final Authenticator AUTHENTICATOR = new Authenticator(new Configuration(), TOKEN_SERVICE);

    @BeforeAll
    static void generateTokens() {
        TOKEN_SERVICE.addToken(AUTH_TOKEN);
    }

    @Test
    void shouldNotAuthWithoutAuthorizationHeader() {
        assertTrue(AUTHENTICATOR.authUri(Collections.emptyMap(), "auth/test").containsError());
    }

    @Test
    void shouldNotAuthUsingOtherAuthMethod() {
        assertTrue(AUTHENTICATOR.authUri(Maps.of("Authorization", "Bearer " + AUTH_TOKEN.getToken()), "auth/test").containsError());
    }

    @Test
    void shouldNotAuthUsingInvalidBasicFormat() {
        assertTrue(AUTHENTICATOR.authUri(Maps.of("Authorization", "Basic"), "auth/test").containsError());
    }

    @Test
    void shouldNotAuthUsingNullCredentials() {
        assertTrue(AUTHENTICATOR.auth((String) null).containsError());
    }

    @Test
    void shouldNotAuthUsingCredentialsWithInvalidFormat() {
        assertTrue(AUTHENTICATOR.auth("alias " + AUTH_TOKEN.getToken()).containsError());
        assertTrue(AUTHENTICATOR.auth("alias:" + AUTH_TOKEN.getToken() + ":whatever").containsError());
        assertTrue(AUTHENTICATOR.auth(":" + AUTH_TOKEN.getToken()).containsError());
    }

    @Test
    void shouldNotAuthUsingInvalidCredentials() {
        assertTrue(AUTHENTICATOR.auth("admin:admin").containsError());
        assertTrue(AUTHENTICATOR.auth("alias:another_secret").containsError());
        assertTrue(AUTHENTICATOR.auth("alias:" + TokenService.B_CRYPT_TOKENS_ENCODER.encode("secret")).containsError());
    }

    @Test
    void shouldAuth() {
        assertTrue(AUTHENTICATOR.auth("alias:secret").isDefined());
        assertTrue(AUTHENTICATOR.auth(Maps.of("Authorization", "Basic " + Base64.getEncoder().encodeToString("alias:secret".getBytes()))).isDefined());
    }

    @Test
    void shouldNotAuthInvalidUri() {
        assertFalse(AUTHENTICATOR.authUri(Maps.of("Authorization", "Basic " + Base64.getEncoder().encodeToString("alias:secret".getBytes())), "auth").isDefined());
    }

    @Test
    void shouldAuthUri() {
        assertTrue(AUTHENTICATOR.authUri(Maps.of("Authorization", "Basic " + Base64.getEncoder().encodeToString("alias:secret".getBytes())), "auth/test").isDefined());
    }

}