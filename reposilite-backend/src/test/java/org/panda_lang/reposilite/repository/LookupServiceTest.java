package org.panda_lang.reposilite.repository;

import io.javalin.http.Context;
import io.javalin.http.util.ContextUtil;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.reposilite.utils.Result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LookupServiceTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturnErrorForInvalidProxiedRequest() {
        super.reposilite.getConfiguration().setProxied(Collections.singletonList(url("").toString()));
        LookupService lookupService = new LookupService(super.reposilite);

        Context context = mockContext("/groupId/artifactId");
        Result<Context, String> result = lookupService.serveProxied(context);

        assertTrue(result.containsError());
        assertEquals("Invalid proxied request", result.getError());
    }

    private Context mockContext(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);

        HttpServletResponse response = mock(HttpServletResponse.class);
        return ContextUtil.init(request, response);
    }

}