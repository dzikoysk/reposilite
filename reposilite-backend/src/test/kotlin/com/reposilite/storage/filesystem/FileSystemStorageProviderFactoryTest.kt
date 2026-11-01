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

package com.reposilite.storage.filesystem

import java.nio.file.attribute.PosixFilePermission
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class FileSystemStorageProviderFactoryTest {

    @Test
    fun `should return an error for invalid file permissions`() {
        val result = FileSystemStorageProviderFactory.parseFilePermissions("888")

        assertThat(assertError(result)).contains("octal value with three digits")
    }

    @Test
    fun `should require owner read permission`() {
        val result = FileSystemStorageProviderFactory.parseFilePermissions("044")

        assertThat(assertError(result)).contains("owner to read")
    }

    @Test
    fun `should accept owner read permission without write permission`() {
        val result = FileSystemStorageProviderFactory.parseFilePermissions("400")

        assertThat(assertOk(result)).containsExactly(PosixFilePermission.OWNER_READ)
    }

}
