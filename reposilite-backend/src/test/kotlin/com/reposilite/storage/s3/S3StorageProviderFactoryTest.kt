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

import com.reposilite.storage.StorageProviderOwner
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

internal class S3StorageProviderFactoryTest {

    private class Registration(var active: Boolean = true)

    private val factory = S3StorageProviderFactory()

    @Test
    fun `should allow distinct buckets endpoints and prefixes`() {
        // given: repositories with distinct S3 namespaces
        register(owner("maven", "releases"), "https://s3.example", "releases", "packages/")
        register(owner("maven", "snapshots"), "https://s3.example", "snapshots", "")
        register(owner("generic", "downloads"), "https://other.example", "releases", "")

        // when & then: another distinct namespace is accepted
        assertThatCode {
            register(owner("generic", "assets"), "https://s3.example", "releases", "assets/")
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should reject equal and nested namespaces owned by another repository`() {
        // given: a registered S3 namespace
        register(owner("maven", "releases"), "https://s3.example", "shared", "artifacts/")

        // when & then: an equal normalized namespace is rejected
        assertThatIllegalArgumentException()
            .isThrownBy {
                register(owner("generic", "downloads"), "https://s3.example/", " shared ", "artifacts/")
            }
            .withMessageContaining("maven:releases")

        // when & then: a nested namespace is rejected
        assertThatIllegalArgumentException()
            .isThrownBy {
                register(owner("generic", "downloads"), "https://s3.example", "shared", "artifacts/releases/")
            }
            .withMessageContaining("maven:releases")
    }

    @Test
    fun `should allow repository replacement and discard inactive registrations`() {
        // given: two generations of the same repository registration
        val owner = owner("maven", "releases")
        val previous = register(owner, "", "shared", "old/")
        val replacement = register(owner, "", "shared", "new/")

        // when: the previous generation becomes inactive
        previous.active = false

        // then: the active replacement still owns its namespace
        assertThatIllegalArgumentException()
            .isThrownBy { register(owner("generic", "downloads"), "", "shared", "new/") }

        // when: the replacement also becomes inactive
        replacement.active = false

        // then: another repository can claim the released namespace
        assertThatCode {
            register(owner("generic", "downloads"), "", "shared", "new/")
        }.doesNotThrowAnyException()
    }

    private fun register(
        owner: StorageProviderOwner,
        endpoint: String,
        bucket: String,
        keyPrefix: String,
    ): Registration =
        Registration().also { registration ->
            factory.registerNamespace(owner, endpoint, bucket, keyPrefix) { registration.active }
        }

    private fun owner(type: String, repository: String): StorageProviderOwner =
        StorageProviderOwner(repositoryType = type, repositoryName = repository)
}
