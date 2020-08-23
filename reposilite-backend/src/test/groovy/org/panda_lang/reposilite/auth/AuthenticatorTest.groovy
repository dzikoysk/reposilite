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

package org.panda_lang.reposilite.auth


import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.AuthenticatorConfiguration
import org.panda_lang.utilities.commons.collection.Maps

import static org.junit.jupiter.api.Assertions.assertTrue

class AuthenticatorTest extends AuthenticatorConfiguration {

    @Test
    void 'should not auth without authorization header' () {
        assertTrue AUTHENTICATOR.authByUri(Collections.emptyMap(), "auth/test").containsError()
    }

    @Test
    void 'should not auth using other auth method' () {
        assertTrue AUTHENTICATOR.authByUri(Maps.of("Authorization", "Bearer " + AUTH_TOKEN.getToken()), "auth/test").containsError()
    }

    @Test
    void 'should not auth using invalid basic format' () {
        assertTrue AUTHENTICATOR.authByUri(Maps.of("Authorization", "Basic"), "auth/test").containsError()
    }

    @Test
    void 'should not auth using null credentials' () {
        assertTrue AUTHENTICATOR.authByCredentials((String) null).containsError()
    }

    @Test
    void 'should not auth using credentials with invalid format' () {
        assertTrue AUTHENTICATOR.authByCredentials("alias " + AUTH_TOKEN.getToken()).containsError()
        assertTrue AUTHENTICATOR.authByCredentials("alias:" + AUTH_TOKEN.getToken() + ":whatever").containsError()
        assertTrue AUTHENTICATOR.authByCredentials(":" + AUTH_TOKEN.getToken()).containsError()
    }

    @Test
    void 'should not auth using invalid credentials' () {
        assertTrue AUTHENTICATOR.authByCredentials("admin:admin").containsError()
        assertTrue AUTHENTICATOR.authByCredentials("alias:another_secret").containsError()
        assertTrue AUTHENTICATOR.authByCredentials("alias:" + TokenService.B_CRYPT_TOKENS_ENCODER.encode("secret")).containsError()
    }

    @Test
    void 'should auth' () {
        assertTrue AUTHENTICATOR.authByCredentials("alias:secret").isDefined()
        assertTrue AUTHENTICATOR.authByHeader(Maps.of("Authorization", BASIC)).isDefined()
    }

    @Test
    void 'should auth context' () {
        assertTrue AUTHENTICATOR.authByUri(Maps.of("Authorization", BASIC), "auth/test").isDefined()
    }

    @Test
    void 'should not auth invalid uri' () {
        assertTrue AUTHENTICATOR.authByUri(Maps.of("Authorization", BASIC), "auth").containsError()
    }

    @Test
    void 'should auth uri' () {
        assertTrue AUTHENTICATOR.authByUri(Maps.of("Authorization", BASIC), "auth/test").isDefined()
    }

}