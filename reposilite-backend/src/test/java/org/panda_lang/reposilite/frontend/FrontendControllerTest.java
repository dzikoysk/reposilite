package org.panda_lang.reposilite.frontend;

import com.google.api.client.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendControllerTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturn200AndJavaScriptContent() throws IOException {
        HttpResponse response = get("http://localhost:80/js/app.js");

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertTrue(response.getContentType().contains("application/javascript"));
    }

}