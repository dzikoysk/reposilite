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

package com.reposilite.maven

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MetadataComparatorTest {

    private data class FileInfo(
        val name: String,
        val isDirectory: Boolean
    )

    private val comparator = FilesComparator<FileInfo>(
        { file -> VersionComparator.DEFAULT_VERSION_PATTERN.split(file.name) },
        { it.isDirectory }
    )

    @Test
    fun `should sort versions in ascending order`() {
        // given: an unordered list of files
        val files = listOf(
            FileInfo("Reposilite", false),
            FileInfo("1.0.3", false),
            FileInfo("1.0.3", true),
            FileInfo("1.0.2", true),
            FileInfo("1.0.1", true)
        )

        // when: an unordered list is sorted
        val result = files.sortedWith(comparator)

        // then: sorted list matches expected rules
        assertEquals(
            listOf(
                FileInfo("1.0.1", true),
                FileInfo("1.0.2", true),
                FileInfo("1.0.3", true),
                FileInfo("1.0.3", false),
                FileInfo("Reposilite", false)
            ),
            result
        )
    }

}