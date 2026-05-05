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
        val n = super.read(b, off, len)
        if (n <= 0) return n
        bytesRead += n
        if (bytesRead > maxBytes) throw BoundedInputStreamLimitExceededException(maxBytes)
        return n
    }

}

class BoundedInputStreamLimitExceededException(val limitBytes: Long) :
    IOException("Stream exceeded the $limitBytes byte limit")
