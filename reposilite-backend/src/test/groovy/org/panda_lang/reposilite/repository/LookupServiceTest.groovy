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

import org.apache.http.HttpStatus
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteContext
import org.panda_lang.reposilite.ReposiliteIntegrationTest

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class LookupServiceTest extends ReposiliteIntegrationTest {

    @Test
    void 'should return 203 and repository not found message' () {
        def repositoryAuthenticator = new RepositoryAuthenticator(
                false, // disable path rewrite option which is enabled by default
                super.reposilite.getAuthenticator(),
                super.reposilite.getRepositoryService())

        def lookupService = new LookupService(
                super.reposilite.getAuthenticator(),
                repositoryAuthenticator,
                super.reposilite.getMetadataService(),
                super.reposilite.getRepositoryService(),
                super.reposilite.getFailureService())

        def context = new ReposiliteContext('/invalid_repository/groupId/artifactId', 'GET', '', [:], { null }, { null })
        def result = lookupService.findLocal(context)
        assertTrue result.containsError()

        def error = result.getError()
        assertEquals HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, error.getStatus()
        assertEquals 'Repository invalid_repository not found', error.getMessage()
    }

}
