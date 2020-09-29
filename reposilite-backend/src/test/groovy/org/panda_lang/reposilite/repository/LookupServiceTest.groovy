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
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteContext
import org.panda_lang.reposilite.ReposiliteTestSpecification

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class LookupServiceTest extends ReposiliteTestSpecification {

    @Test
    void 'should return 203 for directory access' () {
        def context = new ReposiliteContext('/releases/org/panda-lang', 'GET', '', [:], {})
        def result = super.reposilite.getLookupService().findLocal(context)
        assertTrue result.containsError()

        def error = result.getError()
        assertEquals 'Directory access', error.message
        assertEquals HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, error.status
    }

    @Test
    void 'should return 404 for unauthorized request to snapshot metadata file' () {
        def context = new ReposiliteContext('/unauthorized_repository/1.0.0-SNAPSHOT/maven-metadata.xml', 'GET', '', [:], {})
        def result = createLookupService().findLocal(context)
        assertTrue result.containsError()

        def error = result.getError()
        assertEquals HttpStatus.SC_NOT_FOUND, error.status
    }

    @Test
    void 'should return 203 and repository not found message' () {
        def context = new ReposiliteContext('/invalid_repository/groupId/artifactId', 'GET', '', [:], {})
        def result = createLookupService().findLocal(context)
        assertTrue result.containsError()

        def error = result.getError()
        assertEquals HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, error.status
        assertEquals 'Repository invalid_repository not found', error.message
    }

    private LookupService createLookupService() {
        def repositoryAuthenticator = new RepositoryAuthenticator(
                false, // disable path rewrite option which is enabled by default
                super.reposilite.getAuthenticator(),
                super.reposilite.getRepositoryService())

        return new LookupService(
                super.reposilite.getAuthenticator(),
                repositoryAuthenticator,
                super.reposilite.getMetadataService(),
                super.reposilite.getRepositoryService(),
                super.reposilite.ioService,
                super.reposilite.getFailureService())
    }

}
