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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.utilities.commons.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookupControllerTest extends ReposiliteIntegrationTest {

    @TempDir
    File proxiedWorkingDirectory;

    @Test
    void shouldReturn404AndFrontendWithUnsupportedRequestMessage() throws IOException {
        assert404WithMessage(get("/"), "Unsupported request");
    }

    @Test
    void shouldReturn404AndFrontendWithRepositoryNotFoundMessage() throws IOException {
        super.reposilite.getConfiguration().setRewritePathsEnabled(false);
        assert404WithMessage(get("/invalid_repository/groupId/artifactId"), "Repository invalid_repository not found");
    }

    @Test
    void shouldReturn404AndFrontendWithMissingArtifactIdentifier() throws IOException {
        assert404WithMessage(get("/releases/groupId"), "Missing artifact identifier");
    }

    @Test
    void shouldReturn404AndFrontendWithMissingArtifactPathMessage() throws IOException {
        assert404WithMessage(get("/releases/groupId/artifactId"), "Artifact groupId/artifactId not found");
    }

    @Test
    void shouldReturn200AndMetadataFile() throws IOException {
        HttpResponse response = get("/releases/org/panda-lang/reposilite-test/maven-metadata.xml");
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertTrue(response.parseAsString().contains("<version>1.0.0</version>"));
    }

    @Test
    void shouldReturn200AndLatestVersion() throws IOException {
        HttpResponse response = get("/releases/org/panda-lang/reposilite-test/latest");
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals("1.0.1-SNAPSHOT", response.parseAsString());
    }

    @Test
    void shouldReturn404AndFrontendWithLatestVersionNotFound() throws IOException {
        assert404WithMessage(get("/releases/org/panda-lang/reposilite-test/reposilite-test-1.0.0.jar/latest"), "Latest version not found");
    }

    @Test
    void shouldReturn200AndResolvedSnapshotFile() throws IOException {
        HttpResponse response = super.get("/releases/org/panda-lang/reposilite-test/1.0.0-SNAPSHOT/reposilite-test-1.0.0-SNAPSHOT.pom");
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertTrue(response.parseAsString().contains("<version>1.0.0-SNAPSHOT</version>"));
    }

    @Test
    void shouldReturn404AndArtifactNotFoundMessage() throws IOException {
        assert404WithMessage(super.get("/releases/org/panda-lang/reposilite-test/1.0.0/artifactId"), "Artifact org/panda-lang/reposilite-test/1.0.0/artifactId not found");
    }

    @Test
    void shouldReturn200AndRequestedFile() throws IOException {
        HttpResponse response = super.get("/releases/org/panda-lang/reposilite-test/1.0.0/reposilite-test-1.0.0.pom");
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertTrue(response.parseAsString().contains("<version>1.0.0</version>"));
    }

    @Test
    void shouldReturn200AndHeadRequestedFile() throws IOException {
        HttpResponse response = REQUEST_FACTORY
                .buildHeadRequest(super.url("/releases/org/panda-lang/reposilite-test/1.0.0/reposilite-test-1.0.0.pom"))
                .execute();

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertTrue(response.parseAsString().isEmpty());
    }

    @Test
    void shouldReturn404WithUnauthorizedMessage() throws IOException {
        super.reposilite.getConfiguration().setFullAuthEnabled(true);
        assert404WithMessage(super.get("/releases"), "Authorization credentials are not specified");
    }

    @Test
    void shouldReturn200AndProxiedFile() throws Exception {
        String proxyPort = String.valueOf(Integer.parseInt(PORT) + 1);
        super.reposilite.getConfiguration().setProxied(Collections.singletonList("http://localhost:" + proxyPort));

        try {
            Reposilite proxiedReposilite = super.reposilite(proxyPort, proxiedWorkingDirectory);
            proxiedReposilite.launch();

            File proxiedFile = new File(proxiedWorkingDirectory, "/repositories/releases/proxiedGroup/proxiedArtifact/proxied.txt");
            proxiedFile.getParentFile().mkdirs();
            proxiedFile.createNewFile();
            FileUtils.overrideFile(proxiedFile, "proxied content");

            HttpResponse response = get("/releases/proxiedGroup/proxiedArtifact/proxied.txt");
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());

            String content = response.parseAsString();
            assertEquals("proxied content", content);
            System.out.println(content);

            proxiedReposilite.shutdown();
        }
        finally {
            System.clearProperty("reposilite.port");
        }
    }

    static void assert404WithMessage(HttpResponse response, String message) throws IOException {
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
        String content = response.parseAsString();
        System.out.println(content);
        assertTrue(content.contains("REPOSILITE_MESSAGE = '" + message + "'"));
    }

}