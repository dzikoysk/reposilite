package org.panda_lang.reposilite.repository;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexApiControllerTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturn401WithUnauthorizedRequestMessage() throws IOException {
        super.reposilite.getConfiguration().setFullAuthEnabled(true);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, super.get("/api/").getStatusCode());
    }

    @Test
    void shouldReturn200AndLatestFile() {

    }

}