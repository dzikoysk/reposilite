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

@CompileStatic
class AuthDtoTest {

    private static final AuthDto AUTH_DTO = new AuthDto('associated_path', 'm', Collections.singletonList('releases'))

    @Test
    void getRepositories() {
        assertEquals Collections.singletonList('releases'), AUTH_DTO.getRepositories()
    }

    @Test
    void getPath() {
        assertEquals 'associated_path', AUTH_DTO.getPath()
    }

    @Test
    void 'has permission'() {
        assertEquals 'm', AUTH_DTO.getPermissions()
    }

}