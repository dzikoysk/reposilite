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
import org.panda_lang.reposilite.ReposiliteTestSpecification

import static org.junit.jupiter.api.Assertions.*

class KeygenCommandTest extends ReposiliteTestSpecification {

    @Test
    void 'should create new token' () {
        def tokenService = super.reposilite.getTokenService()
        tokenService.createToken("/a", "alias")

        def keygenCommand = new KeygenCommand("/a/b/c", "alias")
        assertTrue(keygenCommand.execute(super.reposilite))

        def token = reposilite.getTokenService().getToken("alias")
        assertNotNull token
        assertEquals "/a/b/c", token.getPath()
        assertEquals "alias", token.getAlias()
        assertNotNull token.getToken()
    }

    @Test
    void 'should create token based on qualifier' () {
        def keygenCommand = new KeygenCommand("org.panda-lang.reposilite", "reposilite")
        assertTrue keygenCommand.execute(super.reposilite)
        assertEquals "*/org/panda-lang/reposilite", super.reposilite.getTokenService().getToken("reposilite").getPath()
    }

    @Test
    void 'should false if file is not available' () {
        def reposilite = super.reposilite
        super.reposilite.getTokenService().createToken("/", "alias")

        executeOnLocked(new File(super.workingDirectory, "tokens.dat"), {
            assertFalse(new KeygenCommand("/a/b/c", "alias").execute(reposilite))
        })

        assertTrue new KeygenCommand("/a/b/c", "alias").execute(reposilite)
    }

}