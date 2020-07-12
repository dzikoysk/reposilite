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

import com.google.api.client.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexApiControllerTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturn401WithUnauthorizedRequestMessage() throws IOException {
        super.reposilite.getConfiguration().setFullAuthEnabled(true);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, super.get("/api/").getStatusCode());
    }

    @Test
    void shouldReturn200AndLatestFile() throws IOException {
        JsonObject result = shouldReturn200AndJsonResponse("/api/org/panda-lang/reposilite-test/latest");
        assertEquals("directory", result.getString("type", null));
        assertEquals("1.0.1-SNAPSHOT", result.getString("name", null));
    }

    @Test
    void shouldReturn404IfRequestedFileIsNotFound() throws IOException {
        HttpResponse response = super.get("/api/org/panda-lang/reposilite-test/unknown");

        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
        assertTrue(response.parseAsString().contains("File not found"));
    }

    @Test
    void shouldReturn200AndFileDto() throws IOException {
        JsonObject result = shouldReturn200AndJsonResponse("/api/org/panda-lang/reposilite-test/1.0.0/reposilite-test-1.0.0.jar");

        assertEquals("file", result.getString("type", null));
        assertEquals("reposilite-test-1.0.0.jar", result.getString("name", null));
    }

    @Test
    void shouldReturn200AndDirectoryDto() throws IOException {
        JsonObject result = shouldReturn200AndJsonResponse("/api/org/panda-lang/reposilite-test");
        assertTrue(result.get("files").isArray());

        JsonArray array = result.get("files").asArray();
        assertEquals("1.0.1-SNAPSHOT", array.get(0).asObject().getString("name", null));
    }

    private JsonObject shouldReturn200AndJsonResponse(String uri) throws IOException {
        HttpResponse response = super.get(uri);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        return  (JsonObject) JsonObject.readJSON(response.parseAsString());
    }

}