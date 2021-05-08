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

package org.panda_lang.reposilite

import groovy.transform.CompileStatic
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.config.Configuration
import org.panda_lang.reposilite.error.FailureService
import org.panda_lang.reposilite.repository.RepositoryService

import java.util.concurrent.Executors

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class ReposiliteUtilsTest {

    @TempDir
    protected static File WORKING_DIRECTORY
    private static RepositoryService REPOSITORY_SERVICE

    @BeforeAll
    static void prepare() {
        REPOSITORY_SERVICE = new RepositoryService(
                WORKING_DIRECTORY.getAbsolutePath(),
                '0',
                Executors.newSingleThreadExecutor(),
                Executors.newSingleThreadScheduledExecutor(),
                new FailureService(),
        )

        REPOSITORY_SERVICE.load(new Configuration())
    }

    @Test
    void 'should not interfere' () {
        assertEquals "releases/without/repo-one/", ReposiliteUtils.normalizeUri(true, REPOSITORY_SERVICE, "releases/without/repo-one/").get()
    }

    @Test
    void 'should rewrite path' () {
        assertEquals "releases/without/repo/", ReposiliteUtils.normalizeUri(true, REPOSITORY_SERVICE, "/without/repo/").get()
    }

    @Test
    void 'should not allow path escapes' () {
        assertTrue ReposiliteUtils.normalizeUri(true, REPOSITORY_SERVICE, "~/home").isEmpty()
        assertTrue ReposiliteUtils.normalizeUri(true, REPOSITORY_SERVICE, "../../../../monkas").isEmpty()
        assertTrue ReposiliteUtils.normalizeUri(true, REPOSITORY_SERVICE, "C:\\").isEmpty()
    }

    @Test
    void 'should not rewrite paths' () {
        assertEquals "without/repo/", ReposiliteUtils.normalizeUri(false, REPOSITORY_SERVICE, "without/repo/").get()
    }

}