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

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

internal class S3StorageProviderFactoryTest {

    private class Registration(var active: Boolean = true)

    private val factory = S3StorageProviderFactory()

    @Test
    fun `should allow distinct buckets endpoints and prefixes`() {
        // given: repositories with distinct S3 namespaces
        register("releases", "https://s3.example", "releases", "packages/")
        register("snapshots", "https://s3.example", "snapshots", "")
        register("downloads", "https://other.example", "releases", "")

        // when & then: another distinct namespace is accepted
        assertThatCode {
            register("assets", "https://s3.example", "releases", "assets/")
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should reject equal and nested active namespaces`() {
        // given: a registered S3 namespace
        register("releases", "https://s3.example", "shared", "artifacts/")

        // when & then: an equal normalized namespace is rejected
        assertThatIllegalArgumentException()
            .isThrownBy {
                register("downloads", "https://s3.example/", " shared ", "artifacts/")
            }
            .withMessageContaining("releases")

        // when & then: a nested namespace is rejected
        assertThatIllegalArgumentException()
            .isThrownBy {
                register("downloads", "https://s3.example", "shared", "artifacts/releases/")
            }
            .withMessageContaining("releases")

        // when & then: the same repository name does not bypass the conflict check
        assertThatIllegalArgumentException()
            .isThrownBy { register("releases", "https://s3.example", "shared", "artifacts/") }
    }

    @Test
    fun `should allow repository replacement and discard inactive registrations`() {
        // given: a registered S3 namespace
        val previous = register("releases", "", "shared", "artifacts/")

        // when: the previous generation becomes inactive
        previous.active = false

        // then: a replacement can reuse the namespace
        val replacement = register("releases", "", "shared", "artifacts/")

        // when & then: another repository cannot claim the active replacement's namespace
        assertThatIllegalArgumentException()
            .isThrownBy { register("downloads", "", "shared", "artifacts/") }

        // when: the replacement also becomes inactive
        replacement.active = false

        // then: another repository can claim the released namespace
        assertThatCode {
            register("downloads", "", "shared", "artifacts/")
        }.doesNotThrowAnyException()
    }

    private fun register(
        repositoryName: String,
        endpoint: String,
        bucket: String,
        keyPrefix: String,
    ): Registration =
        Registration().also { registration ->
            factory.registerNamespace(repositoryName, endpoint, bucket, keyPrefix) { registration.active }
        }
}
