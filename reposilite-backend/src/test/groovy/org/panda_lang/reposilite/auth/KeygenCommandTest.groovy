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
class KeygenCommandTest extends ReposiliteTestSpecification {

    @Test
    void 'should create new token' () {
        def tokenService = super.reposilite.getTokenService()
        tokenService.createToken('/a', 'alias', 'rwm')

        // should also override previous token
        assertTrue executeCommand('keygen /a/b/c alias rw')

        def token = tokenService.getToken('alias').get()
        assertNotNull token
        assertEquals '/a/b/c', token.getPath()
        assertEquals 'alias', token.getAlias()
        assertEquals 'rw', token.getPermissions()
        assertNotNull token.getToken()
    }

    @Test
    void 'should create token based on qualifier' () {
        assertTrue executeCommand('keygen org.panda-lang.reposilite reposilite rw')
        assertEquals '*/org/panda-lang/reposilite', super.reposilite.getTokenService().getToken('reposilite').get().uri()
    }

//    @Test
//    void 'should false if file is not available' () {
//        super.reposilite.getTokenService().createToken('/', 'alias', 'rwm')
//
//        executeOnLocked(new File(super.workingDirectory, 'tokens.dat'), {
//            assertFalse executeCommand('keygen /a/b/c alias rw')
//        })
//
//        assertTrue executeCommand('keygen /a/b/c alias rw')
//    }

}