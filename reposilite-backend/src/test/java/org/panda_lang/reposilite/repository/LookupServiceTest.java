package org.panda_lang.reposilite.repository;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.util.ContextUtil;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.reposilite.utils.FutureUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.FileUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LookupServiceTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturnErrorForInvalidProxiedRequest() {
        super.reposilite.getConfiguration().setProxied(Collections.singletonList(url("").toString()));
        LookupService lookupService = new LookupService(super.reposilite);

        Context context = mockContext("/groupId/artifactId");
        Result<CompletableFuture<Context>, String> result = lookupService.serveProxied(context);

        assertTrue(result.containsError());
        assertEquals("Invalid proxied request", result.getError());
    }

    @Test
    void shouldReturnProxiedFile() throws Exception {
        super.reposilite.getConfiguration().setProxied(Collections.singletonList(url("").toString()));
        LookupService lookupService = new LookupService(super.reposilite);

        File proxiedFile = new File(workingDirectory, "/repositories/releases/proxiedGroup/proxiedArtifact/proxied.txt");
        proxiedFile.getParentFile().mkdirs();
        proxiedFile.createNewFile();
        FileUtils.overrideFile(proxiedFile, "proxied content");

        Context context = mockContext("/releases/proxiedGroup/proxiedArtifact/proxied.txt");

        Future<Context> completableFuture = FutureUtils.submit(Executors.newFixedThreadPool(1), future -> {
            Result<CompletableFuture<Context>, String> result = lookupService.serveProxied(context);

            try {
                return future.complete(result.getValue().get());
            } catch (InterruptedException | ExecutionException e) {
                return future.cancel(true);
            }
        });

        doAnswer(invocation -> {
            assertEquals(HttpStatus.SC_OK, (int) invocation.getArgument(0));
            return null;
        }).when(context.res).setStatus(anyInt());

        completableFuture.get();
    }

    private Context mockContext(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class, Mockito.RETURNS_DEEP_STUBS);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getMethod()).thenReturn("HEAD");

        HttpServletResponse response = mock(HttpServletResponse.class, Mockito.RETURNS_DEEP_STUBS);
        return ContextUtil.init(request, response, "*", Collections.emptyMap(), HandlerType.HEAD);
    }

}