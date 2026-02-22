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

package com.reposilite.token

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AccessTokenSecurityProviderTest {

    @Test
    fun `should match correct secret`() {
        val secret = "correct-password"
        val encoded = AccessTokenSecurityProvider.encodeSecret(secret)
        assertThat(AccessTokenSecurityProvider.matches(encoded, secret)).isTrue
    }

    @Test
    fun `should not match incorrect secret`() {
        val encoded = AccessTokenSecurityProvider.encodeSecret("correct-password")
        assertThat(AccessTokenSecurityProvider.matches(encoded, "wrong-password")).isFalse
    }

    @Test
    fun `should return false for password exceeding bcrypt 72 byte limit`() {
        val encoded = AccessTokenSecurityProvider.encodeSecret("short")
        val longPassword = "a".repeat(256)
        assertThat(AccessTokenSecurityProvider.matches(encoded, longPassword)).isFalse
    }

}
