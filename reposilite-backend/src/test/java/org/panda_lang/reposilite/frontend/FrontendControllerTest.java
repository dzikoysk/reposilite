package org.panda_lang.reposilite.frontend;

import com.google.api.client.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendControllerTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturn200AndJavaScriptContent() throws Exception {
       HttpResponse response = super.get("/js/app.js");
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertTrue(response.getContentType().contains("application/javascript"));
    }

}