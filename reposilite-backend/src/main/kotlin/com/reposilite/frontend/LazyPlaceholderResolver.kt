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
import java.nio.file.Files
import java.nio.file.StandardOpenOption

internal class LazyPlaceholderResolver(private val placeholders: Map<String, String>) {

    init {
        verifyPlaceholders()
    }

    private val theLongestPlaceholder = placeholders.keys
        .maxOfOrNull { it }
        ?.length
        ?: 0

    fun createProcessedResource(input: InputStream): ResourceSupplier {
        val temporaryResourcePath = Files.createTempFile("reposilite", "frontend-resource")

        input.use { inputStream ->
            Files.newOutputStream(temporaryResourcePath, StandardOpenOption.WRITE).use { outputStream ->
                process(inputStream, outputStream)
            }
        }

        return ResourceSupplier {
            Result.supplyThrowing(IOException::class.java) {
                Files.newInputStream(temporaryResourcePath)
            }
        }
    }

    fun process(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024)

        while (true) {
            val length = input.read(buffer)
                .takeIf { it != -1 }
                ?: break

            // convert data from buffer to text
            var content = buffer.decodeToString(endIndex = length)

            // check if current content may end with placeholder
            if (content.length > theLongestPlaceholder) {
                content = loadSlicedPlaceholders(input, content)
            }

            // resolve placeholders
            for ((name, value) in placeholders) {
                content = content.replace(name, value)
            }

            val resolvedContentAsBytes = content.toByteArray()
            output.write(resolvedContentAsBytes, 0, resolvedContentAsBytes.size)
        }
    }

    private fun loadSlicedPlaceholders(input: InputStream, content: String): String {
        val borderlineStartIndex = content.length - theLongestPlaceholder + 1
        val positionCount = content.length - borderlineStartIndex

        for (index in 0 until positionCount) {
            val placeholderStartIndex = borderlineStartIndex + index
            val boundaryPlaceholder = content.substring(placeholderStartIndex, content.length)

            for (placeholder in placeholders.keys) {
                if (placeholder.startsWith(boundaryPlaceholder)) {
                    val missingBuffer = ByteArray(theLongestPlaceholder)
                    val missingLength = input.read(missingBuffer)

                    if (missingLength == -1) {
                        return content
                    }

                    val contentWithMissingPlaceholder = content + missingBuffer.decodeToString(endIndex = missingLength)
                    return loadSlicedPlaceholders(input, contentWithMissingPlaceholder)
                }
            }
        }

        return content
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