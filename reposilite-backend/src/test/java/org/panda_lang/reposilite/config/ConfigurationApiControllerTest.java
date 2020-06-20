package org.panda_lang.reposilite.config;

import com.google.api.client.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.hjson.JsonObject;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigurationApiControllerTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturn200AndConfigurationDto() throws IOException {
        HttpResponse response = super.get("/api/configuration");
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        JsonObject configurationDto = (JsonObject) JsonObject.readJSON(response.parseAsString());
        assertNotNull(configurationDto.getString("title", null));
        assertNotNull(configurationDto.getString("description", null));
        assertNotNull(configurationDto.getString("accentColor", null));
    }

}