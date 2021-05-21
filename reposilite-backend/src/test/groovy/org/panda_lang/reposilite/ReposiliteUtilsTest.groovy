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

package org.panda_lang.reposilite

import groovy.transform.CompileStatic
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.config.Configuration
import org.panda_lang.reposilite.repository.RepositoryService

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class ReposiliteUtilsTest {

    @TempDir
    protected static Path WORKING_DIRECTORY
    private static RepositoryService REPOSITORY_SERVICE

    @BeforeAll
    static void prepare() {
        REPOSITORY_SERVICE = new RepositoryService()

        REPOSITORY_SERVICE.load(new Configuration())
    }

    @Test
    void 'should not interfere' () {
        assertEquals "releases/without/repo-one/", ReposiliteUtils.normalizeUri("releases/without/repo-one/").get()
    }

    @Test
    void 'should rewrite path' () {
        assertEquals "releases/without/repo/", ReposiliteUtils.normalizeUri("/without/repo/").get()
    }

    @Test
    void 'should not allow path escapes' () {
        assertTrue ReposiliteUtils.normalizeUri("~/home").isEmpty()
        assertTrue ReposiliteUtils.normalizeUri("../../../../monkas").isEmpty()
        assertTrue ReposiliteUtils.normalizeUri("C:\\").isEmpty()
    }

    @Test
    void 'should not rewrite paths' () {
        assertEquals "without/repo/", ReposiliteUtils.normalizeUri("without/repo/").get()
    }

}