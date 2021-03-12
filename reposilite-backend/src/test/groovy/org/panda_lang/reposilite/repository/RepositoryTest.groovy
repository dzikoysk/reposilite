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
import org.panda_lang.reposilite.storage.FileSystemStorageProvider

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse

@CompileStatic
class RepositoryTest {

    @TempDir
    protected static Path temp

    private static Repository repository

    @BeforeAll
    static void prepare() {
        repository = new Repository(temp, "releases", false, FileSystemStorageProvider.of(Paths.get(""), "10GB"))
        Files.createDirectories(repository.getFile("group", "artifact", "version"))
        Files.createFile(repository.getFile("group", "artifact", "version", "test"))
    }

    @Test
    void 'should find requested entity' () {
        assertFalse repository.find("unknown").isPresent()
        assertEquals "test", Objects.requireNonNull(repository.find("group", "artifact", "version", "test")).get().getFile("test").getFileName()
    }

    @Test
    void 'should return file' () {
        assertEquals "test", repository.getFile("test").getFileName()
    }

    @Test
    void 'should return repository name' () {
        assertEquals "releases", repository.getName()
    }

}