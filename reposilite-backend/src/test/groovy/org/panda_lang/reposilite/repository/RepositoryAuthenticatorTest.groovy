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

package org.panda_lang.reposilite.repository

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.AuthenticatorSpecification

import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
class RepositoryAuthenticatorTest extends AuthenticatorSpecification {

    static final RepositoryAuthenticator REPOSITORY_AUTHENTICATOR = new RepositoryAuthenticator(true, AUTHENTICATOR, REPOSITORY_SERVICE, STORAGE_PROVIDER)

    @Test
    void 'should not auth invalid repository uri' () {
        assertEquals "Unsupported request", REPOSITORY_AUTHENTICATOR.authRepository([:], '').getError().getMessage()
    }

}
