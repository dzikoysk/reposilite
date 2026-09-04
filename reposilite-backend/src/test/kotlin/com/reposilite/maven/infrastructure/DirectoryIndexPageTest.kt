/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.maven.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DirectoryIndexPageTest {

    @Test
    fun `should link repository root to application root`() {
        // given: a repository index rendered at the application root
        val basePath = "/"
        val repositoryPath = "/downloads"

        // when: the directory index page is created
        val page = createDirectoryIndexPage(basePath, repositoryPath, emptyList())

        // then: the parent link targets the application root
        assertThat(page).contains("<a href='/'>Parent Directory</a>")
    }
}
