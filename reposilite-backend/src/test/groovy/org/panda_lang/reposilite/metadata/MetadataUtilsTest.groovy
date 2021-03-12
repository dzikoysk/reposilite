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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.storage.FileSystemStorageProvider
import org.panda_lang.reposilite.storage.StorageProvider
import org.panda_lang.utilities.commons.ArrayUtils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class MetadataUtilsTest {

    @TempDir
    protected static Path temp

    static StorageProvider storageProvider = FileSystemStorageProvider.of(Paths.get(""), "10GB")

    static Path builds
    static Path versions
    static Path file

    private static final String[] BUILDS = [
            "abc-1.0.0-1337-2-classifier.pom",
            "abc-1.0.0-1337-1-classifier.pom",
            "abc-1.0.0-1337-1.pom",
            "abc-1.0.0-1337-classifier.pom",
            "abc-1.0.0-1337.pom",
    ]

    private static final String[] VERSIONS = [
            "3.0.0",
            "2.11.0",
            "2.9.0",
            "2.8.10",
            "2.8.10-SNAPSHOT",
            "2.07",
            "1"
    ]

    @BeforeAll
    static void prepare() throws IOException {
        builds = temp.resolve("builds")
        Files.createDirectories(builds)

        for (String file : BUILDS) {
            Files.createFile(builds.resolve(file))
        }

        versions = temp.resolve("versions")
        Files.createDirectories(versions)

        for (String version : VERSIONS) {
            Files.createFile(versions.resolve(file))
        }

        file = temp.resolve("file")
        Files.createFile(file)
    }

    @Test
    void 'should sort builds' () {
        assertArrayEquals BUILDS, Stream.of(MetadataUtils.toSortedBuilds(storageProvider, builds).get())
                .map({ file -> file.getFileName() })
                .toArray({ length -> new String[length] })
    }

    @Test
    void 'should map directory to list of files' () {
        assertArrayEquals BUILDS, Stream.of(MetadataUtils.toFiles(storageProvider, builds).get())
                .map({ file -> file.getFileName() })
                .toArray({ length -> new String[length] })
    }

    @Test
    void 'should sort versions' () {
        assertArrayEquals VERSIONS, Stream.of(MetadataUtils.toSortedVersions(storageProvider, versions).get()   )
                .map({ file -> file.getFileName() })
                .toArray({ length -> new String[length] })
    }
//
//    @Test
//    void 'should sort identifiers' () {
//        assertArrayEquals([
//                "1337-2",
//                "1337-1",
//                "1337",
//        ] as String[], MetadataUtils.toSortedIdentifiers("abc", "1.0.0", FilesUtils.listFiles(builds)))
//    }

    @Test
    void toBuildFiles() {
        assertArrayEquals BUILDS, Stream.of(MetadataUtils.toBuildFiles(storageProvider, builds, "1337"))
                .map({ file -> file.getFileName() })
                .toArray({ length -> new String[length] })
    }

    @Test
    void 'should convert date to update time' () {
        assertTrue MetadataUtils.toUpdateTime(storageProvider, file).startsWith(Integer.toString(Calendar.getInstance().get(Calendar.YEAR)))
    }

    @Test
    void 'should convert array of elements to group name' () {
        assertEquals "a.b.c", MetadataUtils.toGroup(ArrayUtils.of("a", "b", "c"))
        assertEquals "a.b.c", MetadataUtils.toGroup(ArrayUtils.of("a", "b", "c", "d", "e"), 2)
    }

}