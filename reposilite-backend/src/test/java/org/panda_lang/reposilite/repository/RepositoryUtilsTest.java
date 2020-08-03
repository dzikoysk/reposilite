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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepositoryUtilsTest {

    @TempDir
    File temp;
    Configuration CONFIGURATION = new Configuration();
    RepositoryService REPOSITORY_SERVICE;

    @BeforeEach
    void prepare() {
        REPOSITORY_SERVICE = new RepositoryService(temp.getAbsolutePath());
        REPOSITORY_SERVICE.load(CONFIGURATION);
    }

    @Test
    void shouldNotInterfere() {
        assertEquals("releases/without/repo-one/", RepositoryUtils.normalizeUri(CONFIGURATION, REPOSITORY_SERVICE, "releases/without/repo-one/"));
    }

    @Test
    void shouldRewritePath() {
        assertEquals("releases/without/repo/", RepositoryUtils.normalizeUri(CONFIGURATION, REPOSITORY_SERVICE, "/without/repo/"));
    }

    @Test
    void shouldNotAllowPathEscapes() {
        assertEquals(StringUtils.EMPTY, RepositoryUtils.normalizeUri(CONFIGURATION, REPOSITORY_SERVICE, "~/home"));
        assertEquals(StringUtils.EMPTY, RepositoryUtils.normalizeUri(CONFIGURATION, REPOSITORY_SERVICE, "../../../../monkas"));
        assertEquals(StringUtils.EMPTY, RepositoryUtils.normalizeUri(CONFIGURATION, REPOSITORY_SERVICE, "C:\\"));
    }

    @Test
    void shouldNotRewritePaths() {
        Configuration configuration = new Configuration();
        configuration.rewritePathsEnabled = false;

        assertEquals("without/repo/", RepositoryUtils.normalizeUri(configuration, REPOSITORY_SERVICE, "without/repo/"));
    }

}