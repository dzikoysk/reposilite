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

package com.reposilite.storage.s3

import com.reposilite.storage.s3.specification.S3StorageProviderFactorySpecification
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class S3StorageProviderFactoryTest : S3StorageProviderFactorySpecification() {

    @Test
    fun `should allow distinct buckets endpoints and prefixes`() {
        // given: repositories with distinct S3 storage paths
        useClient("releases", "https://s3.example", "releases", "packages/")
        useClient("snapshots", "https://s3.example", "snapshots", "")
        useClient("downloads", "https://other.example", "releases", "")

        // when: another repository uses a distinct prefix
        val exception = catchThrowable {
            useClient("assets", "https://s3.example", "releases", "assets/")
        }

        // then: the registration succeeds
        assertThat(exception).isNull()
    }

    @ParameterizedTest
    @CsvSource(
        "downloads, artifacts/",
        "downloads, artifacts/releases/",
        "releases, artifacts/",
    )
    fun `should reject overlapping namespaces`(name: String, prefix: String) {
        // given: a registered repository
        useClient("releases", "https://s3.example", "shared", "artifacts/")

        // when: a repository claims the same or a nested storage path
        val exception = catchThrowable { useClient(name, "https://s3.example", "shared", prefix) }

        // then: the existing repository prevents registration, even under the same name
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("releases")
    }

    @Test
    fun `should detect conflicts after normalizing endpoint and bucket`() {
        // given: a registered repository
        useClient("releases", "https://s3.example", "shared", "artifacts/")

        // when: the same storage is requested with a trailing slash and surrounding whitespace
        val exception = catchThrowable {
            useClient("downloads", "https://s3.example/", " shared ", "artifacts/")
        }

        // then: normalization does not hide the conflict
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("releases")
    }

    @Test
    fun `should release a namespace when its client closes`() {
        // given: two clients using different storage paths
        var closed = false
        val previous = useClient("releases", "", "shared", "artifacts/") { closed = true }
        useClient("files", "", "shared", "files/")

        // when: one client closes and its storage is claimed again
        previous.close()
        val replacement = catchThrowable { useClient("releases", "", "shared", "artifacts/") }
        val conflict = catchThrowable { useClient("other", "", "shared", "files/") }

        // then: its namespace is reusable while the other client's namespace remains reserved
        assertThat(closed).isTrue()
        assertThat(replacement).isNull()
        assertThat(conflict).isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("files")
    }

    @Test
    fun `should keep a replacement registered if the previous client closes again`() {
        // given: a replacement using the same repository name and storage path
        val previous = useClient("releases", "", "shared", "artifacts/")
        previous.close()
        useClient("releases", "", "shared", "artifacts/")

        // when: the previous client is closed again
        previous.close()
        val conflict = catchThrowable { useClient("other", "", "shared", "artifacts/") }

        // then: the replacement still owns its namespace
        assertThat(conflict).isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("releases")
    }

    @Test
    fun `should release a namespace even if closing its client fails`() {
        // given: a client that throws during shutdown
        val client = useClient("releases", "", "shared", "artifacts/") { error("Close failed") }

        // when: the client is closed and another repository requests its storage
        val failure = catchThrowable { client.close() }
        val replacement = catchThrowable { useClient("downloads", "", "shared", "artifacts/") }

        // then: the error is preserved and the old namespace no longer blocks initialization
        assertThat(failure).isInstanceOf(IllegalStateException::class.java).hasMessage("Close failed")
        assertThat(replacement).isNull()
    }

}
