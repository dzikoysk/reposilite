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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.repository.RepositoryService;
import org.panda_lang.utilities.commons.collection.Maps;

import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticatorTest {

    private static final RepositoryService REPOSITORY_SERVICE = new RepositoryService(".", "0");
    private static final TokenService TOKEN_SERVICE = new TokenService(".");
    private static final Token AUTH_TOKEN = new Token("/auth/test", "alias", TokenService.B_CRYPT_TOKENS_ENCODER.encode("secret"));
    private static final String BASIC = "Basic " + Base64.getEncoder().encodeToString("alias:secret".getBytes());
    private static final Authenticator AUTHENTICATOR = new Authenticator(new Configuration(), REPOSITORY_SERVICE, TOKEN_SERVICE);

    @BeforeAll
    static void generateTokens() {
        TOKEN_SERVICE.addToken(AUTH_TOKEN);
    }

    @Test
    void shouldNotAuthWithoutAuthorizationHeader() {
        assertTrue(AUTHENTICATOR.authByUri(Collections.emptyMap(), "auth/test").containsError());
    }

    @Test
    void shouldNotAuthUsingOtherAuthMethod() {
        assertTrue(AUTHENTICATOR.authByUri(Maps.of("Authorization", "Bearer " + AUTH_TOKEN.getToken()), "auth/test").containsError());
    }

    @Test
    void shouldNotAuthUsingInvalidBasicFormat() {
        assertTrue(AUTHENTICATOR.authByUri(Maps.of("Authorization", "Basic"), "auth/test").containsError());
    }

    @Test
    void shouldNotAuthUsingNullCredentials() {
        assertTrue(AUTHENTICATOR.authByCredentials((String) null).containsError());
    }

    @Test
    void shouldNotAuthUsingCredentialsWithInvalidFormat() {
        assertTrue(AUTHENTICATOR.authByCredentials("alias " + AUTH_TOKEN.getToken()).containsError());
        assertTrue(AUTHENTICATOR.authByCredentials("alias:" + AUTH_TOKEN.getToken() + ":whatever").containsError());
        assertTrue(AUTHENTICATOR.authByCredentials(":" + AUTH_TOKEN.getToken()).containsError());
    }

    @Test
    void shouldNotAuthUsingInvalidCredentials() {
        assertTrue(AUTHENTICATOR.authByCredentials("admin:admin").containsError());
        assertTrue(AUTHENTICATOR.authByCredentials("alias:another_secret").containsError());
        assertTrue(AUTHENTICATOR.authByCredentials("alias:" + TokenService.B_CRYPT_TOKENS_ENCODER.encode("secret")).containsError());
    }

    @Test
    void shouldAuth() {
        assertTrue(AUTHENTICATOR.authByCredentials("alias:secret").isDefined());
        assertTrue(AUTHENTICATOR.authByHeader(Maps.of("Authorization", BASIC)).isDefined());
    }

    @Test
    void shouldAuthContext() {
        assertTrue(AUTHENTICATOR.authByUri(Maps.of("Authorization", BASIC), "auth/test").isDefined());
    }

    @Test
    void shouldNotAuthInvalidRepositoryUri() {
        assertEquals("Unsupported request", AUTHENTICATOR.authRepository(null, "").getError().getMessage());
    }

    @Test
    void shouldNotAuthInvalidUri() {
        assertTrue(AUTHENTICATOR.authByUri(Maps.of("Authorization", BASIC), "auth").containsError());
    }

    @Test
    void shouldAuthUri() {
        assertTrue(AUTHENTICATOR.authByUri(Maps.of("Authorization", BASIC), "auth/test").isDefined());
    }

}