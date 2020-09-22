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

package org.panda_lang.reposilite.auth

import groovy.transform.CompileStatic
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.config.Configuration
import org.panda_lang.reposilite.error.FailureService
import org.panda_lang.reposilite.repository.RepositoryService

import java.util.concurrent.Executors

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class SessionTest {

    @TempDir
    protected static File WORKING_DIRECTORY
    protected static RepositoryService REPOSITORY_SERVICE

    @BeforeAll
    static void prepare () {
        REPOSITORY_SERVICE = new RepositoryService(WORKING_DIRECTORY.getAbsolutePath(), "0", Executors.newSingleThreadExecutor(), new FailureService())
        REPOSITORY_SERVICE.load(new Configuration())
    }

    @Test
    void 'has permission' () {
        def configuration = new Configuration()
        configuration.repositories = Collections.emptyList()

        def token = new Token("/a/b/c", "alias", "token")
        def standardSession = new Session(token, false, REPOSITORY_SERVICE.getRepositories(token))

        assertTrue standardSession.hasPermission("/a/b/c")
        assertTrue standardSession.hasPermission("/a/b/c/d")

        assertFalse standardSession.hasPermission("/a/b/")
        assertFalse standardSession.hasPermission("/a/b/d")
    }

    @Test
    void 'has permission with wildcard' () {
        def token = new Token("*/b/c", "alias", "token")
        def wildcardSession = new Session(token, false, REPOSITORY_SERVICE.getRepositories(token))

        assertTrue wildcardSession.hasPermission("/releases/b/c")
        assertTrue wildcardSession.hasPermission("/releases/b/c/d")
        assertTrue wildcardSession.hasPermission("/snapshots/b/c")

        assertFalse wildcardSession.hasPermission("/releases/b")
        assertFalse wildcardSession.hasPermission("/snapshots/b")
        assertFalse wildcardSession.hasPermission("/custom/b/c")
    }

    @Test
    void 'has root permission' () {
        def standardToken = new Token("/", "alias", "token")
        def standardRootSession = new Session(standardToken, false, REPOSITORY_SERVICE.getRepositories(standardToken))

        def wildcardToken = new Token("*", "alias", "token")
        def wildcardRootSession = new Session(wildcardToken, false, REPOSITORY_SERVICE.getRepositories(wildcardToken))

        assertTrue standardRootSession.hasPermission("/")
        assertFalse wildcardRootSession.hasPermission("/")
        assertTrue wildcardRootSession.hasPermission("/releases")
        assertTrue wildcardRootSession.hasPermission("/snapshots")
    }

    @Test
    void 'should contain repository from path' () {
        def token = new Token("/releases", "alias", "token")
        def session = new Session(token, false, REPOSITORY_SERVICE.getRepositories(token))
        assertEquals Collections.singletonList(REPOSITORY_SERVICE.getRepository("releases")), session.getRepositories()
    }

    @Test
    void 'should return empty list for unknown repository in path' () {
        def token = new Token("/unknown_repository", "alias", "token")
        def session = new Session(token, false, REPOSITORY_SERVICE.getRepositories(token))
        assertEquals Collections.emptyList(), session.getRepositories()
    }

}