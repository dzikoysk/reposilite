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

package org.panda_lang.reposilite.metadata

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteTestSpecification
import org.panda_lang.utilities.commons.function.Result

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
final class MetadataServiceTest extends ReposiliteTestSpecification {

    @Test
    void 'should return bad request' () {
        def result = generate("org", "panda-lang", "reposilite-test", "1.0.0", "reposilite-test-1.0.0.jar", "reposilite-test-1.0.0.jar")
        assertEquals "Bad request", result.getError()
    }

    @Test
    void 'should not generate invalid file' () {
        def result = generate("org", "panda-lang", "reposilite-test")
        assertTrue result.isErr()
        assertEquals "Bad request", result.getError()
    }

    @Test
    void 'should return artifact metadata content' () {
        def result = generate("org", "panda-lang", "reposilite-test", "maven-metadata.xml")

        assertTrue result.isOk()
        assertTrue result.get().contains("1.0.0")
        assertTrue result.get().contains("1.0.0-SNAPSHOT")
        assertTrue result.get().contains("1.0.1-SNAPSHOT")
    }

    @Test
    void 'should return builds not found' () {
        Result<String, String> result = generate("org", "panda-lang", "reposilite-test", "1.0.2-SNAPSHOT", "maven-metadata.xml")
        assertEquals "Latest build not found", result.getError()
    }

    @Test
    void 'should return snapshot metadata content' () {
        def result = generate("org", "panda-lang", "reposilite-test", "1.0.0-SNAPSHOT", "maven-metadata.xml")
        assertTrue result.isOk()
        assertTrue result.get().contains("1.0.0-SNAPSHOT")
        assertTrue result.get().contains("<buildNumber>1</buildNumber>")
        assertTrue result.get().contains("<timestamp>20200603.224843</timestamp>")
        assertTrue result.get().contains("<extension>pom</extension>")
        assertTrue result.get().contains("<value>1.0.0-20200603.224843-1</value>")
    }

    @Test
    void 'should return fake snapshot metadata content' () {
        def result = generate("org", "panda-lang", "reposilite-test", "1.0.1-SNAPSHOT", "maven-metadata.xml")
        assertTrue result.isOk()
        assertTrue result.get().contains("<release>1.0.1-SNAPSHOT</release>")
        assertTrue result.get().contains("<latest>1.0.1-SNAPSHOT</latest>")
        assertTrue result.get().contains("<version>1.0.1-SNAPSHOT</version>")
    }

    @Test
    void 'should clear cache' () {
        String[] metadata = [ "org", "panda-lang", "reposilite-test", "maven-metadata.xml" ]
        generate(metadata)

        MetadataService metadataService = super.reposilite.getMetadataService()
        assertEquals 1, metadataService.getCacheSize()

        metadataService.clearMetadata(super.reposilite.getRepositoryService().getRepository("releases").getFile(metadata))
        assertEquals 0, metadataService.getCacheSize()
    }

    @Test
    void 'should purge cache' () {
        def metadataService = super.reposilite.getMetadataService()
        assertEquals 0, metadataService.purgeCache()
        assertEquals 0, metadataService.getCacheSize()

        generateAll()
        assertEquals 4, metadataService.purgeCache()
        assertEquals 0, metadataService.getCacheSize()
    }

    @Test
    void 'should return current cache size' () {
        def metadataService = super.reposilite.getMetadataService()
        assertEquals 0, metadataService.getCacheSize()

        generateAll()
        assertEquals 4, metadataService.getCacheSize()
    }

    private void generateAll() {
        generate "org", "panda-lang", "reposilite-test", "maven-metadata.xml"
        generate "org", "panda-lang", "reposilite-test", "1.0.0", "maven-metadata.xml"
        generate "org", "panda-lang", "reposilite-test", "1.0.0-SNAPSHOT", "maven-metadata.xml"
        generate "org", "panda-lang", "reposilite-test", "1.0.1", "maven-metadata.xml" // should not generate this one (empty dir)
        generate "org", "panda-lang", "reposilite-test", "1.0.1-SNAPSHOT", "maven-metadata.xml"
    }

    private Result<String, String> generate(String... path) {
        def releases = super.reposilite.getRepositoryService().getRepository("releases")
        return super.reposilite.getMetadataService().generateMetadata(releases, path)
    }

}
