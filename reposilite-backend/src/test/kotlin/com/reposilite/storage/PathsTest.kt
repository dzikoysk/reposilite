/*
 * Copyright (c) 2026 dzikoysk
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

package com.reposilite.storage

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeText

class PathsTest {

    @Test
    fun `should delete a directory strictly inside the root`(@TempDir root: Path) {
        val target = root.resolve("group/artifact").also { it.createDirectories() }
        target.resolve("file.txt").writeText("payload")

        target.deleteRecursivelyInside(root)

        assertThat(target.exists()).isFalse
        assertThat(root.exists()).isTrue
    }

    @Test
    fun `should refuse to delete the root itself`(@TempDir root: Path) {
        root.resolve("keep.txt").createFile()

        assertThatThrownBy { root.deleteRecursivelyInside(root) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("strictly inside")

        assertThat(root.resolve("keep.txt").exists()).isTrue
    }

    @Test
    fun `should refuse to delete a path outside the root`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("javadocs").also { it.createDirectories() }
        val outside = tempDir.resolve("plugins").also { it.createDirectories() }
        outside.resolve("malformed.jar").createFile()

        assertThatThrownBy { outside.deleteRecursivelyInside(root) }
            .isInstanceOf(IllegalArgumentException::class.java)

        assertThat(outside.resolve("malformed.jar").exists()).isTrue
    }

    @Test
    fun `should refuse to delete an ancestor of the root`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("javadocs/releases").also { it.createDirectories() }

        assertThatThrownBy { tempDir.deleteRecursivelyInside(root) }
            .isInstanceOf(IllegalArgumentException::class.java)

        assertThat(root.exists()).isTrue
    }

}
