/*
 * Copyright (c) 2021 dzikoysk
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

package org.panda_lang.reposilite.maven.repository

import com.google.api.client.http.HttpResponse
import groovy.transform.CompileStatic
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.ReposiliteIntegrationTestSpecification
import org.panda_lang.utilities.commons.FileUtils

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class LookupControllerTest extends ReposiliteIntegrationTestSpecification {

    @TempDir
    protected File proxiedWorkingDirectory

    {
        super.properties.put("reposilite.repositories", "releases,snapshots,.private")
    }

    @Test
    void 'should return 203 and frontend with unsupported request message' () {
        assertResponseWithMessage getRequest("/"), HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Unsupported request"
    }

    @Test
    void 'should return 404 for missing snapshot metadata file with builds not found message' () {
        def response = getRequest("/gav/1.0.0-SNAPSHOT/maven-metadata.xml")
        assertResponseWithMessage response, HttpStatus.SC_NOT_FOUND, "Latest build not found"
    }

    @Test
    void 'should return 203 and frontend with missing artifact identifier' () {
        assertResponseWithMessage getRequest("/releases/groupId"), HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Missing artifact identifier";
    }

    @Test
    void 'should return 404 and frontend with proxied repositories are not enabled message' () {
        assertResponseWithMessage getRequest("/releases/groupId/artifactId"), HttpStatus.SC_NOT_FOUND, "Artifact artifactId not found"
    }

    @Test
    void 'should return 200 and metadata file' () {
        def response = getRequest("/releases/org/panda-lang/reposilite-test/maven-metadata.xml")
        assertEquals HttpStatus.SC_OK, response.getStatusCode()
        assertTrue response.parseAsString().contains("<version>1.0.0</version>")
    }

    @Test
    void 'should return 200 and latest version' () {
        def response = getRequest("/releases/org/panda-lang/reposilite-test/latest")
        assertEquals HttpStatus.SC_OK, response.getStatusCode()
        assertEquals "1.0.1-SNAPSHOT", response.parseAsString()
    }

    @Test
    void 'should return 404 and frontend with latest version not found' () {
        assertResponseWithMessage(
                getRequest("/releases/org/panda-lang/reposilite-test/reposilite-test-1.0.0.jar/latest"),
                HttpStatus.SC_NOT_FOUND,
                "Latest version not found")
    }

    @Test
    void 'should return 200 and resolved snapshot file' () {
        def response = getRequest("/releases/org/panda-lang/reposilite-test/1.0.0-SNAPSHOT/reposilite-test-1.0.0-SNAPSHOT.pom")
        assertEquals HttpStatus.SC_OK, response.getStatusCode()
        assertTrue response.parseAsString().contains("<version>1.0.0-SNAPSHOT</version>")
    }

    @Test
    void 'should return 404 and artifact not found message' () {
        assertResponseWithMessage(
                getRequest("/releases/org/panda-lang/reposilite-test/1.0.0/artifactId"),
                HttpStatus.SC_NOT_FOUND,
                "Artifact artifactId not found")
    }

    @Test
    void 'should return 200 and requested file' () {
        def response = getRequest("/releases/org/panda-lang/reposilite-test/1.0.0/reposilite-test-1.0.0.pom")
        assertEquals HttpStatus.SC_OK, response.getStatusCode()
        assertTrue response.parseAsString().contains("<version>1.0.0</version>")
    }

    @Test
    void 'should return 200 and head requested file' () {
        def response = REQUEST_FACTORY
                .buildHeadRequest(url("/releases/org/panda-lang/reposilite-test/1.0.0/reposilite-test-1.0.0.pom"))
                .execute()

        assertEquals HttpStatus.SC_OK, response.getStatusCode()
        assertTrue response.parseAsString().isEmpty()
    }

    @Test
    void 'should return 401 with unauthorized message' () {
        assertResponseWithMessage getRequest("/private/a/b"), HttpStatus.SC_UNAUTHORIZED, "Unauthorized request"
    }

    @Test
    void 'should return 200 and proxied file' () {
        def proxyPort = String.valueOf(Integer.parseInt(PORT) + 1)
        super.reposilite.getConfiguration().proxied = Collections.singletonList("http://localhost:" + proxyPort)

        try {
            def proxiedReposilite = super.reposilite(proxyPort, proxiedWorkingDirectory)
            proxiedReposilite.launch()

            def proxiedFile = new File(proxiedWorkingDirectory, "/repositories/releases/proxiedGroup/proxiedArtifact/proxied.txt")
            proxiedFile.getParentFile().mkdirs()
            proxiedFile.createNewFile()
            FileUtils.overrideFile(proxiedFile, "proxied content")

            def response = getRequest("/releases/proxiedGroup/proxiedArtifact/proxied.txt")
            assertEquals HttpStatus.SC_OK, response.getStatusCode()

            def content = response.parseAsString()
            assertEquals "proxied content", content

            proxiedReposilite.forceShutdown()
        }
        finally {
            System.clearProperty("reposilite.port")
        }
    }

    static void assertResponseWithMessage(HttpResponse response, int status, String message) {
        assertEquals(status, response.getStatusCode())
        def content = response.parseAsString()
        assertTrue(content.contains("REPOSILITE_MESSAGE = '" + message + "'"));
    }

}