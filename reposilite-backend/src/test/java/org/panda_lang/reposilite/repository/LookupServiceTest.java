package org.panda_lang.reposilite.repository;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.util.ContextUtil;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.reposilite.utils.FutureUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.FileUtils;
import org.panda_lang.utilities.commons.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LookupServiceTest extends ReposiliteIntegrationTest {

    private static final ExecutorService SERVICE = Executors.newFixedThreadPool(2);

    private LookupService lookupService;
    
    @BeforeEach
    void configure() throws IOException {
        super.reposilite.getConfiguration().setProxied(Collections.singletonList(url("").toString()));
        this.lookupService = new LookupService(super.reposilite);

        File proxiedFile = new File(super.workingDirectory, "/repositories/releases/proxiedGroup/proxiedArtifact/proxied.pom");
        proxiedFile.getParentFile().mkdirs();
        proxiedFile.createNewFile();
        FileUtils.overrideFile(proxiedFile, "proxied content");
    }
    
    @Test
    void shouldReturnErrorForInvalidProxiedRequest() {
        Context context = mockContext("/groupId/artifactId");
        Result<CompletableFuture<Context>, String> result = lookupService.serveProxied(context);

        assertTrue(result.containsError());
        assertEquals("Invalid proxied request", result.getError());
    }

    @Test
    void shouldReturn404AndArtifactNotFound() throws Exception {
        Context context = mockContext("/releases/proxiedGroup/proxiedArtifact/notfound.pom");

        doAnswer(invocation -> {
            assertEquals(HttpStatus.SC_NOT_FOUND, (int) invocation.getArgument(0));
            return null;
        }).when(context.res).setStatus(anyInt());

        FutureUtils.submit(SERVICE, future -> {
            return future.complete(lookupService.serveProxied(context).getValue().get());
        }).get();

        String result = IOUtils.toString(context.resultStream(), StandardCharsets.UTF_8);
        assertNotNull(result);
        assertTrue(result.contains("REPOSILITE_MESSAGE = 'Artifact not found in local and remote repository'"));
    }

    @Test
    void shouldReturn200AndProxiedFile() throws Exception {
        Context context = mockContext("/releases/proxiedGroup/proxiedArtifact/proxied.pom");

        doAnswer(invocation -> {
            assertEquals(HttpStatus.SC_OK, (int) invocation.getArgument(0));
            return null;
        }).when(context.res).setStatus(anyInt());

        FutureUtils.submit(SERVICE, future -> {
            return future.complete(lookupService.serveProxied(context).getValue().get());
        }).get();
    }

    private Context mockContext(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class, Mockito.RETURNS_DEEP_STUBS);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getMethod()).thenReturn("HEAD");

        HttpServletResponse response = mock(HttpServletResponse.class, Mockito.RETURNS_DEEP_STUBS);
        return ContextUtil.init(request, response, "*", Collections.emptyMap(), HandlerType.HEAD);
    }

}