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
        useNamespace("releases", "https://s3.example", "releases", "packages/")
        useNamespace("snapshots", "https://s3.example", "snapshots", "")
        useNamespace("downloads", "https://other.example", "releases", "")

        // when: another repository uses a distinct prefix
        val exception = catchThrowable {
            useNamespace("assets", "https://s3.example", "releases", "assets/")
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
    fun `should reject overlapping active namespaces`(name: String, prefix: String) {
        // given: an active repository
        useNamespace("releases", "https://s3.example", "shared", "artifacts/")

        // when: a repository claims the same or a nested storage path
        val exception = catchThrowable { useNamespace(name, "https://s3.example", "shared", prefix) }

        // then: the existing repository prevents registration, even under the same name
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("releases")
    }

    @Test
    fun `should detect conflicts after normalizing endpoint and bucket`() {
        // given: an active repository
        useNamespace("releases", "https://s3.example", "shared", "artifacts/")

        // when: the same storage is requested with a trailing slash and surrounding whitespace
        val exception = catchThrowable {
            useNamespace("downloads", "https://s3.example/", " shared ", "artifacts/")
        }

        // then: normalization does not hide the conflict
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("releases")
    }

    @Test
    fun `should allow repository replacement and discard inactive registrations`() {
        // given: a repository that has shut down
        val previous = useNamespace("releases", "", "shared", "artifacts/")
        previous.active = false

        // when: its replacement claims the same storage
        useNamespace("releases", "", "shared", "artifacts/")
        val exception = catchThrowable {
            useNamespace("downloads", "", "shared", "artifacts/")
        }

        // then: the replacement now owns the storage
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("releases")
    }

    @Test
    fun `should release storage when a replacement shuts down`() {
        // given: a repository replaced by another active instance
        val previous = useNamespace("releases", "", "shared", "artifacts/")
        previous.active = false
        val replacement = useNamespace("releases", "", "shared", "artifacts/")

        // when: the replacement shuts down and another repository claims the storage
        replacement.active = false
        val exception = catchThrowable {
            useNamespace("downloads", "", "shared", "artifacts/")
        }

        // then: no stale registration prevents reuse
        assertThat(exception).isNull()
    }

}
