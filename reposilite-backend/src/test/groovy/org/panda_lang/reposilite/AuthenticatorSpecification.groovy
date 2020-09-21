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

package org.panda_lang.reposilite

import org.junit.jupiter.api.BeforeAll
import org.panda_lang.reposilite.auth.Authenticator
import org.panda_lang.reposilite.auth.Token
import org.panda_lang.reposilite.auth.TokenService
import org.panda_lang.reposilite.config.Configuration
import org.panda_lang.reposilite.error.FailureService
import org.panda_lang.reposilite.repository.RepositoryService

import java.util.concurrent.Executors

class AuthenticatorSpecification {

    static final RepositoryService REPOSITORY_SERVICE = new RepositoryService(".", "0", Executors.newSingleThreadExecutor(), new FailureService())

    static final TokenService TOKEN_SERVICE = new TokenService(".")
    static final Token AUTH_TOKEN = new Token("/auth/test", "alias", TokenService.B_CRYPT_TOKENS_ENCODER.encode("secret"))
    static final String BASIC = "Basic " + Base64.getEncoder().encodeToString("alias:secret".getBytes())

    static final Authenticator AUTHENTICATOR = new Authenticator(new Configuration(), REPOSITORY_SERVICE, TOKEN_SERVICE)

    @BeforeAll
    static void generateTokens() {
        TOKEN_SERVICE.addToken(AUTH_TOKEN)
    }

}
