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

package com.reposilite.shared.stream

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Caps the number of bytes that can be consumed from the underlying [InputStream]. A stream of
 * exactly [maxBytes] bytes is allowed through cleanly; the (maxBytes + 1)-th byte triggers
 * [BoundedInputStreamLimitExceededException].
 *
 * Not thread-safe — like every [InputStream], it expects to be consumed by a single thread.
 */
class BoundedInputStream(delegate: InputStream, private val maxBytes: Long) : FilterInputStream(delegate) {

    private var bytesRead = 0L

    override fun read(): Int {
        val b = super.read()
        if (b == -1) return b
        bytesRead++
        if (bytesRead > maxBytes) throw BoundedInputStreamLimitExceededException(maxBytes)
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        // Clamp the delegated read so we never pull more than (remaining + 1) bytes off the underlying
        // stream — the +1 still lets us observe overflow on the boundary read without over-consuming.
        val remaining = maxBytes - bytesRead
        val capped = minOf(len.toLong(), remaining + 1).coerceAtLeast(1).toInt()
        val n = super.read(b, off, capped)
        if (n <= 0) return n
        bytesRead += n
        if (bytesRead > maxBytes) throw BoundedInputStreamLimitExceededException(maxBytes)
        return n
    }

    override fun skip(n: Long): Long {
        if (n <= 0) return 0
        val buffer = ByteArray(minOf(n, SKIP_BUFFER_BYTES.toLong()).toInt())
        var skipped = 0L
        while (skipped < n) {
            val read = read(buffer, 0, minOf(buffer.size.toLong(), n - skipped).toInt())
            if (read <= 0) break
            skipped += read
        }
        return skipped
    }

    override fun markSupported(): Boolean = false

    override fun mark(readlimit: Int) {}

    override fun reset(): Nothing = throw IOException("mark/reset not supported")

    private companion object {
        const val SKIP_BUFFER_BYTES = 8 * 1024
    }

}

class BoundedInputStreamLimitExceededException(val limitBytes: Long) :
    IOException("Stream exceeded the $limitBytes byte limit")
