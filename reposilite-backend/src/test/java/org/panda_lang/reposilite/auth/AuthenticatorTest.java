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

import io.javalin.http.Context;
import io.javalin.http.util.ContextUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.repository.RepositoryService;
import org.panda_lang.utilities.commons.collection.Maps;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertTrue(AUTHENTICATOR.auth(Maps.of("Authorization", BASIC)).isDefined());
    }

    @Test
    void shouldAuthContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("auth/test");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("Authorization")));
        when(request.getHeader("Authorization")).thenReturn(BASIC);

        HttpServletResponse response = mock(HttpServletResponse.class);
        Context context = ContextUtil.init(request, response);

        assertTrue(AUTHENTICATOR.authDefault(context).isDefined());
    }

    @Test
    void shouldNotAuthInvalidRepositoryUri() {
        assertEquals("Unsupported request", AUTHENTICATOR.authRepository(null, "").getError().getMessage());
    }

    @Test
    void shouldNotAuthInvalidUri() {
        assertTrue(AUTHENTICATOR.authUri(Maps.of("Authorization", BASIC), "auth").containsError());
    }

    @Test
    void shouldAuthUri() {
        assertTrue(AUTHENTICATOR.authUri(Maps.of("Authorization", BASIC), "auth/test").isDefined());
    }

}