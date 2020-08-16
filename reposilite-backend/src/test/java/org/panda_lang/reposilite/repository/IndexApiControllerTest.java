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
import net.dzikoysk.cdn.CDN;
import net.dzikoysk.cdn.model.Configuration;
import net.dzikoysk.cdn.model.Section;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.reposilite.auth.Token;
import org.panda_lang.utilities.commons.collection.Pair;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IndexApiControllerTest extends ReposiliteIntegrationTest {

    {
        super.properties.put("reposilite.repositories", "releases,snapshots,.private");
    }

    @Test
    void shouldReturnListOfRepositories() throws IOException {
        Configuration repositories = shouldReturn200AndJsonResponse("/api");
        assertNotNull(repositories.get("files"));

        Section files = repositories.getSection("files");
        assertEquals(2, files.size());
        assertEquals("releases", files.getSection(0).getString("name"));
        assertEquals("snapshots", files.getSection(1).getString("name"));
    }

    @Test
    void shouldReturnListOfAllAuthenticatedRepositories() throws IOException {
        Pair<String, Token> secret = super.reposilite.getTokenService().createToken("/private", "secret");

        HttpResponse response = super.getAuthenticated("/api", "secret", secret.getKey());
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        Configuration repositories = CDN.defaultInstance().parseJson(response.parseAsString());
        Section files = repositories.getSection("files");
        assertEquals(3, files.size());
        assertEquals("releases", files.getSection(0).getString("name"));
        assertEquals("snapshots", files.getSection(1).getString("name"));
        assertEquals("private", files.getSection(2).getString("name"));
    }

    @Test
    void shouldReturn200AndLatestFile() throws IOException {
        Section result = shouldReturn200AndJsonResponse("/api/org/panda-lang/reposilite-test/latest");
        assertEquals("directory", result.getString("type"));
        assertEquals("1.0.1-SNAPSHOT", result.getString("name"));
    }

    @Test
    void shouldReturn404IfRequestedFileIsNotFound() throws IOException {
        HttpResponse response = super.get("/api/org/panda-lang/reposilite-test/unknown");
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
        assertTrue(response.parseAsString().contains("File not found"));
    }

    @Test
    void shouldReturn200AndFileDto() throws IOException {
        Section result = shouldReturn200AndJsonResponse("/api/org/panda-lang/reposilite-test/1.0.0/reposilite-test-1.0.0.jar");
        assertEquals("file", result.getString("type"));
        assertEquals("reposilite-test-1.0.0.jar", result.getString("name"));
    }

    @Test
    void shouldReturn200AndDirectoryDto() throws IOException {
        Section result = shouldReturn200AndJsonResponse("/api/org/panda-lang/reposilite-test");
        Section files = result.getSection("files");
        assertEquals("1.0.1-SNAPSHOT", files.getSection(0).getString("name"));
    }

    private Configuration shouldReturn200AndJsonResponse(String uri) throws IOException {
        HttpResponse response = super.get(uri);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        return CDN.defaultInstance().parseJson(response.parseAsString());
    }

}