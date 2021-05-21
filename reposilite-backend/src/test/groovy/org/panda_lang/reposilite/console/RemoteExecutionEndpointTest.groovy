/*
 * Copyright (c) 2021 dzikoysk
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

package org.panda_lang.reposilite.console

import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.JsonObjectParser
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ObjectParser
import groovy.transform.CompileStatic
import org.apache.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteIntegrationTestSpecification
import org.panda_lang.reposilite.console.api.RemoteExecutionResponse
import org.panda_lang.utilities.commons.StringUtils
import org.panda_lang.utilities.commons.collection.Pair

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class RemoteExecutionEndpointTest extends ReposiliteIntegrationTestSpecification {

    private static final String MANAGER_ALIAS = 'manager'
    private static final String MANAGER_TOKEN = 'secret'

    private static final String STANDARD_ALIAS = 'user'
    private static final String STANDARD_TOKEN = 'secret'

    private static final ObjectParser PARSER = new JsonObjectParser(new JacksonFactory());

    @BeforeEach
    void prepare () {
        super.reposilite.getTokenService().createToken('', MANAGER_ALIAS, 'm', MANAGER_TOKEN);
        super.reposilite.getTokenService().createToken('', STANDARD_ALIAS, '', STANDARD_TOKEN);
    }

    @Test
    void 'should reject unauthorized requests' () {
        def response = execute('', '', 'tokens')
        assertEquals HttpStatus.SC_UNAUTHORIZED, response.getKey()
    }

    @Test
    void 'should reject tokens without manager permission' () {
        def response = execute(STANDARD_TOKEN, STANDARD_ALIAS, 'tokens')
        assertEquals HttpStatus.SC_UNAUTHORIZED, response.getKey()
    }

    @Test
    void 'should invoke remote command' () {
        def response = execute(MANAGER_ALIAS, MANAGER_TOKEN, 'tokens')
        assertEquals HttpStatus.SC_OK, response.getKey()

        def result = response.getValue()
        assertTrue result.succeeded

        def value = result.response.toString()
        assertTrue value.contains(MANAGER_ALIAS)
        assertTrue value.contains(STANDARD_ALIAS)
    }

    @Test
    void 'should inform about missing command' () {
        def response = execute(MANAGER_ALIAS, MANAGER_TOKEN, '')
        assertEquals HttpStatus.SC_BAD_REQUEST, response.getKey()
    }

    @Test
    void 'should reject long commands' () {
        def response = execute(MANAGER_ALIAS, MANAGER_TOKEN, StringUtils.repeated(1024 + 1, '.'))
        assertEquals HttpStatus.SC_BAD_REQUEST, response.getKey()
    }

    private static Pair<Integer, RemoteExecutionResponse> execute(String alias, String token, String command) {
        def request = REQUEST_FACTORY.buildPostRequest(url('/api/execute'), ByteArrayContent.fromString("text/plain", command))
        request.setThrowExceptionOnExecuteError(false)
        request.headers.setBasicAuthentication(alias, token)
        request.setParser(PARSER)

        def response = request.execute()
        return new Pair<>(response.statusCode, response.isSuccessStatusCode() ? response.parseAs(RemoteExecutionResponse.class) : null)
    }

}
