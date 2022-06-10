/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.shared.fs

import com.reposilite.shared.fs.specification.FileComparatorsSpecification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FileComparatorsTest : FileComparatorsSpecification() {

    @Test
    fun `should sort versions in ascending order`() {
        // given: an unordered list of files
        val files = listOf(
            file("Apposite"),
            file("Lolita"),
            directory("Reposilite"),
            directory("Zeolite"),
            file("1.0.3"),
            file("1.0.2"),
            directory("1.0.2"),
            directory("1.0.1")
        )

        // when: an unordered list is sorted
        val sortedResult = files.sortedWith(filesComparator)

        // then: sorted list matches expected rules
        assertEquals(
            listOf(
                directory("1.0.1"),
                directory("1.0.2"),
                directory("Reposilite"),
                directory("Zeolite"),
                file("1.0.2"),
                file("1.0.3"),
                file("Apposite"),
                file("Lolita"),
            ),
            sortedResult
        )
    }

}