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

package org.panda_lang.reposilite.repository;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.util.ContextUtil;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.reposilite.api.ErrorDto;
import org.panda_lang.reposilite.utils.FutureUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.FileUtils;
import org.panda_lang.utilities.commons.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
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
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void configure() throws IOException {
        super.reposilite.getConfiguration().proxied = Collections.singletonList(url("").toString());
        this.lookupService = new LookupService(super.reposilite);

        File proxiedFile = new File(super.workingDirectory, "/repositories/releases/proxiedGroup/proxiedArtifact/proxied.pom");
        proxiedFile.getParentFile().mkdirs();
        proxiedFile.createNewFile();
        FileUtils.overrideFile(proxiedFile, "proxied content");
    }
    
    @Test
    void shouldReturnErrorForInvalidProxiedRequest() {
        Context context = mockContext("/groupId/artifactId");
        Result<CompletableFuture<Context>, ErrorDto> result = lookupService.serveProxied(context);

        assertTrue(result.containsError());
        assertEquals("Invalid proxied request", result.getError().getMessage());
    }

    @Test
    void shouldReturn404AndArtifactNotFound() throws Exception {
        Context context = mockContext("/releases/proxiedGroup/proxiedArtifact/notfound.pom");

        doAnswer(invocation -> {
            assertEquals(HttpStatus.SC_NOT_FOUND, (int) invocation.getArgument(0));
            return null;
        }).when(context.res).setStatus(anyInt());

        FutureUtils.submit(reposilite, SERVICE, future -> {
            return future.complete(lookupService.serveProxied(context).getValue().get());
        }).get();

        String result = IOUtils.convertStreamToString(context.resultStream()).getValue();
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

        FutureUtils.submit(reposilite, SERVICE, future -> {
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