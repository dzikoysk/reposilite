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

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RepositoryTest {

    @TempDir
    static File temp;
    static Repository repository;

    @BeforeAll
    static void prepare() throws IOException {
        repository = new Repository(temp, "releases");
        repository.getFile("group", "artifact", "version").mkdirs();
        repository.getFile("group", "artifact", "version", "test").createNewFile();
    }

    @Test
    void get() {
        assertNull(repository.get("unknown"));
        assertEquals("test", Objects.requireNonNull(repository.get("group", "artifact", "version", "test")).getFile("test").getName());
    }

    @Test
    void getFile() {
        assertEquals("test", repository.getFile("test").getName());
    }

    @Test
    void getName() {
        assertEquals("releases", repository.getName());
    }

}