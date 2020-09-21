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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactTest {

    @TempDir
    static File temp;
    static Repository repository;
    static Artifact artifact;

    @BeforeAll
    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void prepare() throws IOException {
        repository = new Repository(temp, "releases", false);

        File build1 = repository.getFile("groupId", "artifactId", "version", "build1");
        build1.getParentFile().mkdirs();
        build1.createNewFile();

        File build2 = repository.getFile("groupId", "artifactId", "version", "build2");
        build2.getParentFile().mkdirs();
        build2.createNewFile();

        artifact = new Artifact(repository, "groupId", "artifactId", "version");
    }

    @Test
    void getFile() {
        String fileName = ContentJoiner
                .on(File.separator)
                .join(artifact.getRepository().getName(), artifact.getGroup(), artifact.getArtifact(), artifact.getVersion())
                .toString();

        assertEquals(new File(temp, fileName), artifact.getFile(""));
    }

    @Test
    void getLocalPath() {
        assertEquals("groupId/artifactId/version/", artifact.getLocalPath());
    }

    @Test
    void getVersion() {
        assertEquals("version", artifact.getVersion());
    }

    @Test
    void getArtifact() {
        assertEquals("artifactId", artifact.getArtifact());
    }

    @Test
    void getGroup() {
        assertEquals("groupId", artifact.getGroup());
    }

    @Test
    void getRepository() {
        assertEquals("releases", artifact.getRepository().getName());
    }

}