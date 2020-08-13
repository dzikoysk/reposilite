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

package org.panda_lang.reposilite.metadata;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.reposilite.repository.Repository;
import org.panda_lang.reposilite.utils.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MetadataServiceTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturnBadRequest() {
        Result<String, String> result = generate("org", "panda-lang", "reposilite-test", "1.0.0", "reposilite-test-1.0.0.jar", "reposilite-test-1.0.0.jar");
        assertEquals("Bad request", result.getError());
    }

    @Test
    void shouldNotGenerateInvalidFile() {
        Result<String, String> result = generate("org", "panda-lang", "reposilite-test");
        assertTrue(result.containsError());
        assertEquals("Bad request", result.getError());
    }

    @Test
    void shouldReturnArtifactMetadataContent() {
        Result<String, String> result = generate("org", "panda-lang", "reposilite-test", "maven-metadata.xml");

        assertTrue(result.isDefined());
        assertTrue(result.getValue().contains("1.0.0"));
        assertTrue(result.getValue().contains("1.0.0-SNAPSHOT"));
        assertTrue(result.getValue().contains("1.0.1-SNAPSHOT"));
    }

    @Test
    void shouldReturnBuildsNotFound() {
        Result<String, String> result = generate("org", "panda-lang", "reposilite-test", "1.0.2-SNAPSHOT", "maven-metadata.xml");
        assertEquals("Latest build not found", result.getError());
    }

    @Test
    void shouldReturnSnapshotMetadataContent() {
        Result<String, String> result = generate("org", "panda-lang", "reposilite-test", "1.0.0-SNAPSHOT", "maven-metadata.xml");
        assertTrue(result.isDefined());
        assertTrue(result.getValue().contains("1.0.0-SNAPSHOT"));
        assertTrue(result.getValue().contains("<buildNumber>1</buildNumber>"));
        assertTrue(result.getValue().contains("<timestamp>20200603.224843</timestamp>"));
        assertTrue(result.getValue().contains("<extension>pom</extension>"));
        assertTrue(result.getValue().contains("<value>1.0.0-20200603.224843-1</value>"));
    }

    @Test
    void shouldReturnFakeSnapshotMetadataContent() {
        Result<String, String> result = generate("org", "panda-lang", "reposilite-test", "1.0.1-SNAPSHOT", "maven-metadata.xml");
        assertTrue(result.isDefined());
        assertTrue(result.getValue().contains("<release>1.0.1-SNAPSHOT</release>"));
        assertTrue(result.getValue().contains("<latest>1.0.1-SNAPSHOT</latest>"));
        assertTrue(result.getValue().contains("<version>1.0.1-SNAPSHOT</version>"));
    }

    @Test
    void shouldClearCache() {
        String[] metadata = new String[] { "org", "panda-lang", "reposilite-test", "maven-metadata.xml" };
        generate(metadata);

        MetadataService metadataService = super.reposilite.getMetadataService();
        assertEquals(1, metadataService.getCacheSize());

        metadataService.clearMetadata(super.reposilite.getRepositoryService().getRepository("releases").getFile(metadata));
        assertEquals(0, metadataService.getCacheSize());
    }

    @Test
    void shouldPurgeCache() {
        MetadataService metadataService = super.reposilite.getMetadataService();
        assertEquals(0, metadataService.purgeCache());
        assertEquals(0, metadataService.getCacheSize());

        generateAll();
        assertEquals(4, metadataService.purgeCache());
        assertEquals(0, metadataService.getCacheSize());
    }

    @Test
    void shouldReturnCurrentCacheSize() {
        MetadataService metadataService = super.reposilite.getMetadataService();
        assertEquals(0, metadataService.getCacheSize());

        generateAll();
        assertEquals(4, metadataService.getCacheSize());
    }

    private void generateAll() {
        generate("org", "panda-lang", "reposilite-test", "maven-metadata.xml");
        generate("org", "panda-lang", "reposilite-test", "1.0.0", "maven-metadata.xml");
        generate("org", "panda-lang", "reposilite-test", "1.0.0-SNAPSHOT", "maven-metadata.xml");
        generate("org", "panda-lang", "reposilite-test", "1.0.1", "maven-metadata.xml"); // should not generate this one (empty dir)
        generate("org", "panda-lang", "reposilite-test", "1.0.1-SNAPSHOT", "maven-metadata.xml");
    }

    private Result<String, String> generate(String... path) {
        Repository releases = super.reposilite.getRepositoryService().getRepository("releases");
        return super.reposilite.getMetadataService().generateMetadata(releases, path);
    }

}
