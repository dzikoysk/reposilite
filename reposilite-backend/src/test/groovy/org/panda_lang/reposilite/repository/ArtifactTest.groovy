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

package org.panda_lang.reposilite.repository

import groovy.transform.CompileStatic
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.utilities.commons.text.Joiner

import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
class ArtifactTest {

    @TempDir
    protected static File WORKING_DIRECTORY

    static Repository REPOSITORY
    static Artifact ARTIFACT

    @BeforeAll
    static void prepare() throws IOException {
        REPOSITORY = new Repository(WORKING_DIRECTORY, "releases", false)

        def build1 = REPOSITORY.getFile("groupId", "artifactId", "version", "build1")
        build1.getParentFile().mkdirs()
        build1.createNewFile()

        def build2 = REPOSITORY.getFile("groupId", "artifactId", "version", "build2")
        build2.getParentFile().mkdirs()
        build2.createNewFile()

        ARTIFACT = new Artifact(REPOSITORY, "groupId", "artifactId", "version")
    }

    @Test
    void 'should return artifact file' () {
        def fileName = Joiner.on(File.separator)
                .join(ARTIFACT.getRepository().getName(), ARTIFACT.getGroup(), ARTIFACT.getArtifact(), ARTIFACT.getVersion())
                .toString()

        assertEquals new File(WORKING_DIRECTORY, fileName), ARTIFACT.getFile("")
    }

    @Test
    void 'should return local path' () {
        assertEquals "groupId/artifactId/version/", ARTIFACT.getLocalPath()
    }

    @Test
    void 'should return version' () {
        assertEquals "version", ARTIFACT.getVersion()
    }

    @Test
    void 'should return artifact' () {
        assertEquals "artifactId", ARTIFACT.getArtifact()
    }

    @Test
    void 'should return group' () {
        assertEquals "groupId", ARTIFACT.getGroup()
    }

    @Test
    void 'should return repository' () {
        assertEquals "releases", ARTIFACT.getRepository().getName()
    }

}