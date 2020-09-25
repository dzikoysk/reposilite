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

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assertions.assertFalse

@CompileStatic
class TokenTest {

    @Test
    void 'is wildcard' () {
        def standard = new Token('/a/b/c', 'standard', 'rw', 'secret')
        assertFalse standard.isWildcard()

        def wildcard = new Token('*/a/b/c', 'wildcard', 'rw', 'secret')
        assertTrue wildcard.isWildcard()
    }

    @Test
    void 'has multi-access' () {
        def multiaccess = new Token('/', 'multiaccess', 'rw', 'secret')
        assertTrue multiaccess.hasMultiaccess()

        def multiaccessWildcard = new Token('*', 'multiaccess', 'rw', 'secret')
        assertTrue multiaccessWildcard.hasMultiaccess()
    }

    @Test
    void 'should support deserialization' () {
        def token = new Token()

        token.setPath('/a/b/c');
        assertEquals('/a/b/c', token.getPath())

        token.setAlias('alias')
        assertEquals('alias', token.getAlias())

        token.setToken('secret')
        assertEquals 'secret', token.getToken()
    }

}