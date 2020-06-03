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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.reposilite.utils.FilesUtils;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

class MetadataUtilsTest {

    @TempDir
    static File temp;
    static File builds;
    static File versions;

    private static final String[] BUILDS = {
            "abc-1.0.0-1337.jar",
            "abc-1.0.0-1337-classifier.jar",
            "abc-1.0.0-1337-2-classifier.jar",
            "abc-1.0.0-1337-1.jar",
            "abc-1.0.0-1337-1-classifier.jar"
    };

    private static final String[] VERSIONS = {
            "3.0.0",
            "2.11.0",
            "2.9.0",
            "2.8.10",
            "2.8.10-SNAPSHOT",
            "2.07",
            "1"
    };

    @BeforeAll
    static void prepare() throws IOException {
        builds = new File(temp, "builds");
        builds.mkdir();

        for (String file : BUILDS) {
            new File(builds, file).createNewFile();
        }

        versions = new File(temp, "versions");
        versions.mkdir();

        for (String version : VERSIONS) {
            new File(versions, version).mkdir();
        }
    }

    @Test
    void toSortedBuilds() {
        Assertions.assertArrayEquals(BUILDS, Stream.of(MetadataUtils.toSortedBuilds(builds))
                .map(File::getName)
                .toArray(String[]::new));
    }

    @Test
    void toSortedVersions() {
        Assertions.assertArrayEquals(VERSIONS, Stream.of(MetadataUtils.toSortedVersions(versions))
                .map(File::getName)
                .toArray(String[]::new));
    }

    @Test
    void toSortedIdentifiers() {
        Assertions.assertArrayEquals(new String[] {
                "1337-2",
                "1337-1",
                "1337"
        }, MetadataUtils.toSortedIdentifiers("abc", "1.0.0", FilesUtils.listFiles(builds)));
    }

    @Test
    void toBuildFiles() {
        Assertions.assertArrayEquals(BUILDS, Stream.of(MetadataUtils.toBuildFiles(builds, "1337"))
                .map(File::getName)
                .toArray(String[]::new));
    }

}