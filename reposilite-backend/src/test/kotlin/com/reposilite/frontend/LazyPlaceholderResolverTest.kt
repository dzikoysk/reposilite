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

package com.reposilite.frontend

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream

class LazyPlaceholderResolverTest {

    private val bufferSize = 1024
    private val placeholder =  "{{PLACEHOLDER}}"
    private val defaultResolver = LazyPlaceholderResolver(mapOf(placeholder to "Reposilite"))

    @Test
    fun `should support only 1-byte long symbols in placeholders`() {
        assertThrows<UnsupportedOperationException> {
            LazyPlaceholderResolver(mapOf("🎃" to "Reposilite"))
        }
    }

    @Test
    fun `should resolve placeholder`() {
        val input = placeholder.byteInputStream()
        val output = ByteArrayOutputStream()

        defaultResolver.process(input, output)
        val result = output.toByteArray().decodeToString()

        assertThat(result).contains("Reposilite")
    }

    @Test
    fun `should resolve full length placeholder`() {
        val placeholderBytes = placeholder.toByteArray()
        val input = (ByteArray(bufferSize - placeholderBytes.size + 1) + placeholderBytes + ByteArray(bufferSize - 1)).inputStream()
        val output = ByteArrayOutputStream()

        defaultResolver.process(input, output)
        val result = output.toByteArray().decodeToString()

        assertThat(result).contains("Reposilite")
    }

    @Test
    fun `should resolve one byte length placeholder`() {
        val placeholderBytes = placeholder.toByteArray()
        val input = (ByteArray(bufferSize - 1) + placeholderBytes + ByteArray(bufferSize - 1)).inputStream()
        val output = ByteArrayOutputStream()

        defaultResolver.process(input, output)
        val result = output.toByteArray().decodeToString()

        assertThat(result).contains("Reposilite")
    }

    @Test
    fun `should resolve the longest placeholder split across buffers`() {
        val encodedPlaceholder = "%7B%7BREPOSILITE.VITE_BASE_PATH%7D%7D"
        val resolver = LazyPlaceholderResolver(
            mapOf(
                "{{REPOSILITE.VITE_BASE_PATH}}" to ".",
                encodedPlaceholder to "."
            )
        )
        val prefix = "a".repeat(bufferSize - encodedPlaceholder.length + 1)
        val input = (prefix + encodedPlaceholder).byteInputStream()
        val output = ByteArrayOutputStream()

        resolver.process(input, output)

        assertThat(output.toByteArray().decodeToString()).isEqualTo("$prefix.")
    }

}
