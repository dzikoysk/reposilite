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

package org.panda_lang.reposilite.repository


import groovy.transform.CompileStatic
import net.dzikoysk.cdn.CdnFactory
import net.dzikoysk.cdn.model.Configuration
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteIntegrationTestSpecification

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class LookupApiEndpointTest extends ReposiliteIntegrationTestSpecification {

    {
        super.properties.put('reposilite.repositories', 'releases,snapshots,.private')
    }

    @Test
    void 'should return list of repositories' () {
        def repositories = shouldReturn200AndJsonResponse('/api')
        assertNotNull repositories.get('files')

        def files = repositories.getSection('files').get()
        assertEquals 2, files.size()
        assertEquals 'releases', files.getSection(0).get().getString('name', null)
        assertEquals 'snapshots', files.getSection(1).get().getString('name', null)
    }

    @Test
    void 'should return list of all authenticated repositories' () {
        def secret = super.reposilite.getTokenService().createToken('/private', 'secret', 'rwm')

        def response = getAuthenticated('/api', 'secret', secret.getKey())
        assertEquals HttpStatus.SC_OK, response.getStatusCode()

        def repositories = CdnFactory.createJson().load(response.parseAsString())
        def files = repositories.getSection('files').get()
        assertEquals 3, files.size()
        assertEquals 'releases', files.getSection(0).get().getString('name').get()
        assertEquals 'snapshots', files.getSection(1).get().getString('name').get()
        assertEquals 'private', files.getSection(2).get().getString('name').get()
    }

    @Test
    void 'should return 200 and latest file' () {
        def result = shouldReturn200AndJsonResponse('/api/org/panda-lang/reposilite-test/latest')
        assertEquals 'directory', result.getString('type').get()
        assertEquals '1.0.1-SNAPSHOT', result.getString('name').get()
    }

    @Test
    void 'should return 404 if requested file is not found' () {
        def response = getRequest('/api/org/panda-lang/reposilite-test/unknown')
        assertEquals HttpStatus.SC_NOT_FOUND, response.getStatusCode()
        assertTrue response.parseAsString().contains('File not found')
    }

    @Test
    void 'should return 200 and file dto' () {
        def result = shouldReturn200AndJsonResponse('/api/org/panda-lang/reposilite-test/1.0.0/reposilite-test-1.0.0.jar')
        assertEquals 'file', result.getString('type').get()
        assertEquals 'reposilite-test-1.0.0.jar', result.getString('name').get()
    }

    @Test
    void 'should return 200 and directory dto' () {
        def result = shouldReturn200AndJsonResponse('/api/org/panda-lang/reposilite-test')
        def files = result.getSection('files').get()
        assertEquals '1.0.1-SNAPSHOT', files.getSection(0).get().getString('name').get()
    }

    private static Configuration shouldReturn200AndJsonResponse(String uri) {
        def response = getRequest(uri)
        assertEquals HttpStatus.SC_OK, response.getStatusCode()
        return CdnFactory.createJson().load(response.parseAsString())
    }

}