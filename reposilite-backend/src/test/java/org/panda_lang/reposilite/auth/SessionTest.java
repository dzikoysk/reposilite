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
import org.panda_lang.reposilite.repository.RepositoryService;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class SessionTest {

    @TempDir
    static File temp;
    static RepositoryService REPOSITORY_SERVICE;

    @BeforeAll
    static void prepare() {
        REPOSITORY_SERVICE = new RepositoryService(temp.getAbsolutePath());
        REPOSITORY_SERVICE.load(new Configuration());
    }

    @Test
    void hasPermission() {
        Configuration configuration = new Configuration();
        configuration.setRepositories(Collections.emptyList());
        Session standardSession = new Session(REPOSITORY_SERVICE, new Token("/a/b/c", "alias", "token"), false);

        assertTrue(standardSession.hasPermission("/a/b/c"));
        assertTrue(standardSession.hasPermission("/a/b/c/d"));

        assertFalse(standardSession.hasPermission("/a/b/"));
        assertFalse(standardSession.hasPermission("/a/b/d"));
    }

    @Test
    void hasPermissionWithWildcard() {
        Session wildcardSession = new Session(REPOSITORY_SERVICE, new Token("*/b/c", "alias", "token"), false);

        assertTrue(wildcardSession.hasPermission("/releases/b/c"));
        assertTrue(wildcardSession.hasPermission("/releases/b/c/d"));
        assertTrue(wildcardSession.hasPermission("/snapshots/b/c"));

        assertFalse(wildcardSession.hasPermission("/releases/b"));
        assertFalse(wildcardSession.hasPermission("/snapshots/b"));
        assertFalse(wildcardSession.hasPermission("/custom/b/c"));
    }

    @Test
    void hasRootPermission() {
        Session standardRootSession = new Session(REPOSITORY_SERVICE, new Token("/", "alias", "token"), false);
        Session wildcardRootSession = new Session(REPOSITORY_SERVICE, new Token("*", "alias", "token"), false);

        assertTrue(standardRootSession.hasPermission("/"));
        assertFalse(wildcardRootSession.hasPermission("/"));
        assertTrue(wildcardRootSession.hasPermission("/releases"));
        assertTrue(wildcardRootSession.hasPermission("/snapshots"));
    }

    @Test
    void shouldContainRepositoryFromPath() {
        Session session = new Session(REPOSITORY_SERVICE, new Token("/releases", "alias", "token"), false);
        assertEquals(Collections.singletonList(REPOSITORY_SERVICE.getRepository("releases")), session.getRepositories());
    }

    @Test
    void shouldReturnEmptyListForUnknownRepositoryInPath() {
        Session session = new Session(REPOSITORY_SERVICE, new Token("/unknown_repository", "alias", "token"), false);
        assertEquals(Collections.emptyList(), session.getRepositories());
    }

}