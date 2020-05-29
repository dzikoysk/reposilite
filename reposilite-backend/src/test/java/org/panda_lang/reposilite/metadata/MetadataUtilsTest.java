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
            "abc-version-timestamp.jar",
            "abc-version-timestamp-classifier.jar",
            "abc-version-timestamp-2-classifier.jar",
            "abc-version-timestamp-1.jar",
            "abc-version-timestamp-1-classifier.jar",
    };

    private static final String[] VERSIONS = { "2", "1" };

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
                .toArray(String[]::new)
        );
    }

    @Test
    void toSortedVersions() {
        Assertions.assertArrayEquals(VERSIONS, Stream.of(MetadataUtils.toSortedVersions(versions))
                .map(File::getName)
                .toArray(String[]::new)
        );
    }

    @Test
    void toSortedIdentifiers() {
        Assertions.assertArrayEquals(new String[] {
                "timestamp-2",
                "timestamp-1",
                "timestamp"
        }, MetadataUtils.toSortedIdentifiers("abc", "version", FilesUtils.listFiles(builds)));
    }

    @Test
    void toBuildFiles() {
        Assertions.assertArrayEquals(BUILDS, Stream.of(MetadataUtils.toBuildFiles(builds, "timestamp"))
                .map(File::getName)
                .toArray(String[]::new)
        );
    }

    @Test
    void getLatest() {
        Assertions.assertEquals("2", MetadataUtils.getLatest(VERSIONS));
    }

    @Test
    void getLast() {
        Assertions.assertEquals("1", MetadataUtils.getLast(VERSIONS));
    }

}