package org.panda_lang.reposilite.auth;

import com.google.api.client.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.utilities.commons.ArrayUtils;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuthApiControllerTest extends ReposiliteIntegrationTest {

    @BeforeEach
    void generateToken() {
        reposilite.getTokenService().createToken("/", "admin", "secret");
        reposilite.getConfiguration().setManagers(Collections.singletonList("admin"));
    }

    @Test
    void shouldReturn401WithoutCredentials() throws IOException {
        assertEquals(HttpStatus.SC_UNAUTHORIZED, super.get("/api/auth").getStatusCode());
    }

    @Test
    void shouldReturn401ForInvalidCredentials() throws IOException {
        assertEquals(HttpStatus.SC_UNAUTHORIZED, super.getAuthenticated("/api/auth", "admin", "giga_secret").getStatusCode());
    }

    @Test
    void shouldReturn200AndAuthDto() throws IOException {
        HttpResponse response = super.getAuthenticated("/api/auth", "admin", "secret");
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        JsonObject authDto = (JsonObject) JsonObject.readJSON(response.parseAsString());
        assertTrue(authDto.getBoolean("manager", false));
        assertEquals("/", authDto.getString("path", null));

        assertArrayEquals(ArrayUtils.of("releases", "snapshots"), authDto.get("repositories")
                .asArray().values().stream()
                .map(JsonValue::asString)
                .toArray(String[]::new));
    }

}
