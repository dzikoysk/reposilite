package com.reposilite.frontend

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream

class LazyPlaceholderResolverTest {

    private val bufferSize = 8192
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

        assertTrue(result.contains("Reposilite"))
    }

    @Test
    fun `should resolve full length placeholder`() {
        val placeholderBytes = placeholder.toByteArray()
        val input = (ByteArray(bufferSize - placeholderBytes.size + 1) + placeholderBytes + ByteArray(bufferSize - 1)).inputStream()
        val output = ByteArrayOutputStream()

        defaultResolver.process(input, output)
        val result = output.toByteArray().decodeToString()

        assertTrue(result.contains("Reposilite"))
    }

    @Test
    fun `should resolve one byte length placeholder`() {
        val placeholderBytes = placeholder.toByteArray()
        val input = (ByteArray(bufferSize - 1) + placeholderBytes + ByteArray(bufferSize - 1)).inputStream()
        val output = ByteArrayOutputStream()

        defaultResolver.process(input, output)
        val result = output.toByteArray().decodeToString()

        assertTrue(result.contains("Reposilite"))
    }

}