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
