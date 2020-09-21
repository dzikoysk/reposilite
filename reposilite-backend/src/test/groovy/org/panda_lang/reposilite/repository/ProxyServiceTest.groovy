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

import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.util.ContextUtil
import org.apache.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteTest
import org.panda_lang.reposilite.error.FailureService
import org.panda_lang.utilities.commons.FileUtils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ExecutorService

import static org.junit.jupiter.api.Assertions.*
import static org.mockito.ArgumentMatchers.anyInt
import static org.mockito.Mockito.*

final class ProxyServiceTest extends ReposiliteTest {

    private ExecutorService executorService
    private FailureService failureService
    private ProxyService proxyService

    @BeforeEach
    void configure() throws IOException {
        super.reposilite.getConfiguration().proxied = Collections.singletonList('http://localhost/')

        this.executorService = super.reposilite.getExecutorService()
        this.failureService = super.reposilite.getFailureService()
        this.proxyService = new ProxyService(
                true,
                true,
                [],
                super.reposilite.getExecutorService(),
                failureService,
                super.reposilite.getRepositoryService())

        def proxiedFile = new File(super.workingDirectory, '/repositories/releases/proxiedGroup/proxiedArtifact/proxied.pom')
        proxiedFile.getParentFile().mkdirs()
        proxiedFile.createNewFile()
        FileUtils.overrideFile(proxiedFile, 'proxied content')
    }

    @Test
    void 'should return error for invalid proxied request' () {
        def context = mockContext '/groupId/artifactId'
        def reposiliteContext = super.reposilite.getContextFactory().create(context)
        def result = proxyService.findProxied(reposiliteContext)

        assertTrue result.containsError()
        assertEquals 'Invalid proxied request', result.getError().getMessage()
    }

    @Test
    void 'should return 404 and artifact not found' () throws Exception {
        def context = mockContext '/releases/proxiedGroup/proxiedArtifact/notfound.pom'

        doAnswer({ invocation ->
            assertEquals HttpStatus.SC_NOT_FOUND, (int) invocation.getArgument(0)
            return null
        }).when(context.res).setStatus(anyInt())

        def reposiliteContext = super.reposilite.getContextFactory().create(context)
        def error = proxyService.findProxied(reposiliteContext).getValue().get().getError()
        assertNotNull error
        assertEquals 'Artifact not found in local and remote repository', error.message
    }

    @Test
    void 'should return 200 and proxied file' () throws Exception {
        def context = mockContext '/releases/proxiedGroup/proxiedArtifact/proxied.pom'

        doAnswer({ invocation ->
            assertEquals(HttpStatus.SC_OK, (int) invocation.getArgument(0))
            return null
        }).when(context.res).setStatus anyInt()

        def reposiliteContext = super.reposilite.getContextFactory().create(context)

        executorService.submit({
            return proxyService.findProxied(reposiliteContext).getValue().get().getValue()
        }).get()
    }

    private static Context mockContext(String uri) {
        def request = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS)
        when(request.getRequestURI()).thenReturn(uri)
        when(request.getMethod()).thenReturn('HEAD')

        def response = mock HttpServletResponse.class, RETURNS_DEEP_STUBS
        return ContextUtil.init(request, response, '*', Collections.emptyMap(), HandlerType.HEAD)
    }

}
