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

package com.reposilite.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class SecretTest {

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\u0000", "secret\u0000secret"])
    fun `should reject blank or NUL-containing secrets`(value: String) {
        // given: a blank or NUL-containing value

        // when: a secret is created
        val result = Secret.create(value)

        // then: the invalid value is rejected
        assertError(result)
    }

    @ParameterizedTest
    @ValueSource(strings = ["secret", " secret "])
    fun `should preserve valid secrets`(value: String) {
        // given: a valid value, optionally surrounded by spaces

        // when: a secret is created
        val result = Secret.create(value)

        // then: the secret retains the exact value
        assertThat(assertOk(result).value).isEqualTo(value)
    }

}
