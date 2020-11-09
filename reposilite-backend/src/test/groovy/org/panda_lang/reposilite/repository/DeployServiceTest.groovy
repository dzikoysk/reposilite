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
import org.panda_lang.reposilite.error.FailureService

import java.util.concurrent.Executors

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class DeployServiceTest extends ReposiliteTestSpecification {

    private static final String ALIAS = 'user'
    private static final String TOKEN = 'secret'

    @Test
    void 'should respect disk quota' () {
        def deployService = new DeployService(
                true,
                super.reposilite.authenticator,
                new RepositoryService(
                        super.workingDirectory.getAbsolutePath(),
                        '0MB',
                        Executors.newSingleThreadExecutor(),
                        Executors.newSingleThreadScheduledExecutor(),
                        new FailureService(),
                ),
                super.reposilite.metadataService)

        super.reposilite.tokenService.createToken('/', ALIAS, 'rw', TOKEN)
        def context = createAuthenticatedContext('/releases/a/b/c.txt')

        def result = deployService.deploy(context)
        assertTrue result.containsError()

        def error = result.getError()
        assertEquals HttpStatus.SC_INSUFFICIENT_STORAGE, error.status
        assertEquals 'Out of disk space', error.message
    }

    @Test
    void 'should check write permission' () {
        super.reposilite.getTokenService().createToken('/', ALIAS, 'r', TOKEN)
        def deployService = super.reposilite.getDeployService()

        def context = createAuthenticatedContext('/releases/a/b/c.txt')
        def result = deployService.deploy(context)
        assertTrue result.containsError()

        def error = result.getError()
        assertEquals HttpStatus.SC_UNAUTHORIZED, error.status
        assertEquals 'Cannot deploy artifact without write permission', error.message
    }

    private static ReposiliteContext createAuthenticatedContext(String uri) {
        def auth = [ 'Authorization': 'Basic ' + (ALIAS + ':' + TOKEN).bytes.encodeBase64() ]
        return new ReposiliteContext(uri, 'POST', '', auth, { new ByteArrayInputStream('test'.bytes) })
    }

}
