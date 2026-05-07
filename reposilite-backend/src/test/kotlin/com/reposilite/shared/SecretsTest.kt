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

package com.reposilite.shared

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SecretsTest {

    @Test
    fun `should fully mask null and empty secrets`() {
        assertThat(maskSecret(null)).isEqualTo("<empty>")
        assertThat(maskSecret("")).isEqualTo("<empty>")
    }

    @Test
    fun `should fully mask secrets shorter than the partial-mask threshold`() {
        assertThat(maskSecret("a")).isEqualTo("*")
        assertThat(maskSecret("ab")).isEqualTo("**")
        assertThat(maskSecret("abcd")).isEqualTo("****")
    }

    @Test
    fun `should keep first and last character of secrets at or above the threshold`() {
        assertThat(maskSecret("abcde")).isEqualTo("a***e")
        assertThat(maskSecret("short")).isEqualTo("s***t")
        // AWS access key (20 chars)
        assertThat(maskSecret("AKIAIOSFODNN7EXAMPLE")).isEqualTo("A******************E")
        // AWS secret key (40 chars)
        assertThat(maskSecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")).isEqualTo("w**************************************Y")
    }

    @Test
    fun `should replace each whitespace character with a marker`() {
        assertThat(maskSecret(" AKIAIOSFODNN7EXAMPLE")).isEqualTo("<whitespace>A******************E")
        assertThat(maskSecret("AKIAIOSFODNN7EXAMPLE ")).isEqualTo("A******************E<whitespace>")
        assertThat(maskSecret(" AKIAIOSFODNN7EXAMPLE ")).isEqualTo("<whitespace>A******************E<whitespace>")
        assertThat(maskSecret("\tAKIAIOSFODNN7EXAMPLE\n")).isEqualTo("<whitespace>A******************E<whitespace>")
        assertThat(maskSecret("   abcde")).isEqualTo("<whitespace><whitespace><whitespace>a***e")
        assertThat(maskSecret(" ")).isEqualTo("<whitespace>")
        assertThat(maskSecret("  ")).isEqualTo("<whitespace><whitespace>")
    }

    @Test
    fun `should preserve core length in the mask`() {
        assertThat(maskSecret("x".repeat(8))).hasSize(8)
        assertThat(maskSecret("x".repeat(40))).hasSize(40)
        // boundary whitespace adds markers but the core mask width still matches the trimmed length
        assertThat(maskSecret("  ${"x".repeat(20)}  ").replace("<whitespace>", "")).hasSize(20)
    }

}
