/*
 * Copyright (c) 2020 Dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.panda_lang.reposilite.auth

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteTestSpecification

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class RevokeCommandTest extends ReposiliteTestSpecification {

    @Test
    void 'should revoke existing token' () {
        def tokenService = reposilite.getTokenService()
        tokenService.addToken(new Token('path', 'alias', 'rw', 'secret'))

        assertTrue executeCommand('revoke alias')
        assertNull tokenService.getToken('alias')
    }

    @Test
    void shouldFalseIfTokenDoesNotExist() {
        assertFalse executeCommand('revoke unknown_token')
    }

    @Test
    void shouldFalseIfFileIsNotAvailable() throws Exception {
        super.reposilite.getTokenService().addToken(new Token('path', 'alias', 'rw', 'secret'))

        executeOnLocked(new File(super.workingDirectory, 'tokens.dat'), {
            assertFalse executeCommand('revoke alias')
        })

        assertTrue executeCommand('revoke alias')
    }

}