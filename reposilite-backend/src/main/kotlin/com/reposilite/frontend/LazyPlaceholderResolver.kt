/*
 * Copyright (c) 2023 dzikoysk
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

import panda.std.Result
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.MalformedInputException
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.io.path.createTempFile
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

internal class LazyPlaceholderResolver(private val placeholders: Map<String, String>) {

    init {
        verifyPlaceholders()
    }

    private val theLongestPlaceholderInBytes = placeholders.keys
        .maxOfOrNull { it }
        ?.toByteArray(Charsets.UTF_8)
        ?.size
        ?: 0

    fun createProcessedResource(input: InputStream): ResourceSupplier {
        val temporaryResourcePath = createTempFile("reposilite", "frontend-resource")

        input.use { inputStream ->
            temporaryResourcePath.outputStream().use { outputStream ->
                process(inputStream, outputStream)
            }
        }

        return ResourceSupplier {
            Result.supplyThrowing(IOException::class.java) {
                temporaryResourcePath.inputStream()
            }
        }
    }

    fun process(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024)

        while (true) {
            var readLength = input.read(buffer)
                .takeIf { it != -1 }
                ?: break

            // check if current content may end with placeholder
            val processedBuffer = when (readLength) {
                buffer.size -> loadSlicedPlaceholders(input, buffer).also { readLength = it.size }
                else -> buffer
            }

            try {
                // try to decode content
                var content = processedBuffer.decodeToString(
                    endIndex = readLength,
                    throwOnInvalidSequence = true
                )

                // resolve placeholders
                for ((name, value) in placeholders) {
                    content = content.replace(name, value)
                }

                // save processed content
                val resolvedContentAsBytes = content.toByteArray(UTF_8)
                output.write(resolvedContentAsBytes, 0, resolvedContentAsBytes.size)
            } catch (exception: MalformedInputException) {
                // given frame may contain invalid UTF-8 sequence, so we cannot decode it
                output.write(processedBuffer, 0, readLength)
            }
        }
    }

    private fun loadSlicedPlaceholders(input: InputStream, buffer: ByteArray): ByteArray {
        val borderlineStartIndex = buffer.size - theLongestPlaceholderInBytes + 1
        val positionCount = buffer.size - borderlineStartIndex

        for (index in 0 until positionCount) {
            val placeholderStartIndex = borderlineStartIndex + index
            val boundaryPlaceholder = buffer.copyOfRange(placeholderStartIndex, buffer.size).decodeToString() // .substring(placeholderStartIndex, content.length)

            for (placeholder in placeholders.keys) {
                if (placeholder.startsWith(boundaryPlaceholder)) {
                    val missingBuffer = ByteArray(theLongestPlaceholderInBytes)
                    val missingLength = input.read(missingBuffer)

                    if (missingLength == -1) {
                        return buffer
                    }

                    val contentWithMissingPlaceholder = buffer + missingBuffer
                    return loadSlicedPlaceholders(input, contentWithMissingPlaceholder)
                }
            }
        }

        return buffer
    }

    private fun verifyPlaceholders() {
        placeholders.keys.forEach { placeholder ->
            placeholder.forEach {
                if (it.code > Byte.MAX_VALUE) {
                    throw UnsupportedOperationException("LazyPlaceholderResolve supports only basic placeholders from 1-byte long symbols")
                }
            }
        }
    }

}