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

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.auth.AuthenticationException
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.HttpClients
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteIntegrationTest
import org.panda_lang.utilities.commons.IOUtils
import org.panda_lang.utilities.commons.StringUtils

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

class DeployControllerTest extends ReposiliteIntegrationTest {

    private final def client = HttpClients.createDefault()

    @BeforeEach
    void configure() {
        super.reposilite.getTokenService().createToken("/releases/auth/test", "authtest", "secure")
    }

    @Test
    void 'should return 405 and artifact deployment is disabled message' () throws Exception {
        super.reposilite.getConfiguration().deployEnabled = false
        shouldReturnErrorWithGivenMessage "/releases/groupId/artifactId/file", "authtest", "secure", "content", HttpStatus.SC_METHOD_NOT_ALLOWED, "Artifact deployment is disabled"
    }

    @Test
    void 'should return 401 and invalid credentials message' () throws Exception {
        shouldReturnErrorWithGivenMessage "/releases/groupId/artifactId/file", "authtest", "invalid_token", "content", HttpStatus.SC_UNAUTHORIZED, "Invalid authorization credentials"
    }

    @Test
    void 'should return 200 and success message for metadata files' () throws IOException, AuthenticationException {
        shouldReturn200AndSuccessMessage "/releases/auth/test/maven-metadata.xml", "authtest", "secure", StringUtils.EMPTY
    }

    @Test
    void 'should return 200 and success message'() throws IOException, AuthenticationException {
        shouldReturn200AndSuccessMessage "/releases/auth/test/pom.xml", "authtest", "secure", "maven metadata content"
    }

    private void shouldReturn200AndSuccessMessage(String uri, String username, String password, String content) throws IOException, AuthenticationException {
        def deployResponse = put(uri, username, password, content)
        assertEquals HttpStatus.SC_OK, deployResponse.getStatusLine().getStatusCode()

        if (StringUtils.isEmpty(content)) {
            return;
        }

        assertEquals HttpStatus.SC_OK, super.getAuthenticated(uri, username, password).getStatusCode()
        assertEquals content, super.getAuthenticated(uri, username, password).parseAsString()
    }

    private void shouldReturnErrorWithGivenMessage(String uri, String username, String password, String content, int status, String message) throws Exception {
        def response = put(uri, username, password, content)
        assertEquals status, response.getStatusLine().getStatusCode()

        def result = IOUtils.convertStreamToString(response.getEntity().getContent()).getValue()
        assertNotNull result
        assertTrue result.contains(message)
    }

    private HttpResponse put(String uri, String username, String password, String content) throws IOException, AuthenticationException {
        def httpPut = new HttpPut(url(uri).toString())
        httpPut.setEntity(new StringEntity(content))
        httpPut.addHeader(new BasicScheme().authenticate(new UsernamePasswordCredentials(username, password), httpPut, null))
        return client.execute(httpPut)
    }

}