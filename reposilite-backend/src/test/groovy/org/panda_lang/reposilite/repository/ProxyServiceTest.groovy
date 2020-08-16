package org.panda_lang.reposilite.repository

import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.util.ContextUtil
import org.apache.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteIntegrationTest
import org.panda_lang.reposilite.utils.FutureUtils
import org.panda_lang.utilities.commons.FileUtils
import org.panda_lang.utilities.commons.IOUtils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static org.junit.jupiter.api.Assertions.*
import static org.mockito.ArgumentMatchers.anyInt
import static org.mockito.Mockito.*

final class ProxyServiceTest extends ReposiliteIntegrationTest {

    private static final ExecutorService SERVICE = Executors.newFixedThreadPool(2)
    private ProxyService proxyService

    @BeforeEach
    void configure() throws IOException {
        super.reposilite.getConfiguration().proxied = Collections.singletonList(url('').toString())
        this.proxyService = new ProxyService(super.reposilite)

        def proxiedFile = new File(super.workingDirectory, '/repositories/releases/proxiedGroup/proxiedArtifact/proxied.pom')
        proxiedFile.getParentFile().mkdirs()
        proxiedFile.createNewFile()
        FileUtils.overrideFile(proxiedFile, 'proxied content')
    }

    @Test
    void 'should return error for invalid proxied request' () {
        def context = mockContext '/groupId/artifactId'
        def result = proxyService.findProxied(context)

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

        FutureUtils.submit(reposilite, SERVICE, { future ->
            return future.complete(proxyService.findProxied(context).getValue().get())
        }).get()

        def result = IOUtils.convertStreamToString(context.resultStream()).getValue()
        assertNotNull result
        assertTrue result.contains("REPOSILITE_MESSAGE = 'Artifact not found in local and remote repository'")
    }

    @Test
    void 'should return 200 and proxied file' () throws Exception {
        def context = mockContext '/releases/proxiedGroup/proxiedArtifact/proxied.pom'

        doAnswer({ invocation ->
            assertEquals(HttpStatus.SC_OK, (int) invocation.getArgument(0))
            return null
        }).when(context.res).setStatus anyInt()

        FutureUtils.submit(reposilite, SERVICE, { future ->
            return future.complete(proxyService.findProxied(context).getValue().get())
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
