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

package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.repository.RepositoryService;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class SessionTest {

    @TempDir
    static File temp;
    static RepositoryService REPOSITORY_SERVICE;

    @BeforeAll
    static void prepare() {
        REPOSITORY_SERVICE = new RepositoryService(temp.getAbsolutePath(), "0", Executors.newSingleThreadExecutor(), new FailureService());
        REPOSITORY_SERVICE.load(new Configuration());
    }

    @Test
    void hasPermission() {
        Configuration configuration = new Configuration();
        configuration.repositories = Collections.emptyList();

        Token token = new Token("/a/b/c", "alias", "token");
        Session standardSession = new Session(token, false, REPOSITORY_SERVICE.getRepositories(token));

        assertTrue(standardSession.hasPermission("/a/b/c"));
        assertTrue(standardSession.hasPermission("/a/b/c/d"));

        assertFalse(standardSession.hasPermission("/a/b/"));
        assertFalse(standardSession.hasPermission("/a/b/d"));
    }

    @Test
    void hasPermissionWithWildcard() {
        Token token = new Token("*/b/c", "alias", "token");
        Session wildcardSession = new Session(token, false, REPOSITORY_SERVICE.getRepositories(token));

        assertTrue(wildcardSession.hasPermission("/releases/b/c"));
        assertTrue(wildcardSession.hasPermission("/releases/b/c/d"));
        assertTrue(wildcardSession.hasPermission("/snapshots/b/c"));

        assertFalse(wildcardSession.hasPermission("/releases/b"));
        assertFalse(wildcardSession.hasPermission("/snapshots/b"));
        assertFalse(wildcardSession.hasPermission("/custom/b/c"));
    }

    @Test
    void hasRootPermission() {
        Token standardToken = new Token("/", "alias", "token");
        Session standardRootSession = new Session(standardToken, false, REPOSITORY_SERVICE.getRepositories(standardToken));

        Token wildcardToken = new Token("*", "alias", "token");
        Session wildcardRootSession = new Session(wildcardToken, false, REPOSITORY_SERVICE.getRepositories(wildcardToken));

        assertTrue(standardRootSession.hasPermission("/"));
        assertFalse(wildcardRootSession.hasPermission("/"));
        assertTrue(wildcardRootSession.hasPermission("/releases"));
        assertTrue(wildcardRootSession.hasPermission("/snapshots"));
    }

    @Test
    void shouldContainRepositoryFromPath() {
        Token token = new Token("/releases", "alias", "token");
        Session session = new Session(token, false, REPOSITORY_SERVICE.getRepositories(token));
        assertEquals(Collections.singletonList(REPOSITORY_SERVICE.getRepository("releases")), session.getRepositories());
    }

    @Test
    void shouldReturnEmptyListForUnknownRepositoryInPath() {
        Token token = new Token("/unknown_repository", "alias", "token");
        Session session = new Session(token, false, REPOSITORY_SERVICE.getRepositories(token));
        assertEquals(Collections.emptyList(), session.getRepositories());
    }

}