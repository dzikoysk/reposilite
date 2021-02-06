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

import groovy.transform.CompileStatic
import net.dzikoysk.cdn.CDN
import org.apache.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteIntegrationTestSpecification

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
final class AuthEndpointTest extends ReposiliteIntegrationTestSpecification {

    @BeforeEach
    void generateToken() {
        reposilite.getTokenService().createToken('/', 'admin', 'rwm', 'secret')
    }

    @Test
    void 'should return 401 without credentials' () throws IOException {
        def response = getRequest('/api/auth')
        assertEquals HttpStatus.SC_UNAUTHORIZED, response.getStatusCode()
        assertTrue response.getHeaders().containsKey(PostAuthHandler.WWW_AUTHENTICATE)
    }

    @Test
    void 'should return 401 for invalid credentials' () throws IOException {
        def response = getAuthenticated('/api/auth', 'admin', 'giga_secret')
        assertEquals HttpStatus.SC_UNAUTHORIZED, response.getStatusCode()
        assertTrue response.getHeaders().containsKey(PostAuthHandler.WWW_AUTHENTICATE)
    }

    @Test
    void 'should return 200 and auth dto' () throws IOException {
        def response = getAuthenticated('/api/auth', 'admin', 'secret')
        assertEquals HttpStatus.SC_OK, response.getStatusCode()

        def authDto = CDN.defaultInstance().parseJson(response.parseAsString())
        assertEquals 'rwm', authDto.getString('permissions').get()
        assertEquals '/', authDto.getString('path').get()
        assertEquals Arrays.asList('releases', 'snapshots'), authDto.getList('repositories', [])
    }

}
