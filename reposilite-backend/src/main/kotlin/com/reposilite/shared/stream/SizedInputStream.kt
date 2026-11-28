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

package com.reposilite.shared.stream

import com.reposilite.storage.api.UNKNOWN_LENGTH
import java.io.FilterInputStream
import java.io.InputStream

internal val InputStream.contentLength: Long
    get() = (this as? SizedInputStream)?.remaining ?: UNKNOWN_LENGTH

internal class SizedInputStream(source: InputStream, length: Long) : FilterInputStream(source) {

    var remaining = length
        private set

    private var marked = length

    override fun read(): Int =
        super.read().also { if (it >= 0) remaining-- }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { if (it > 0) remaining -= it }

    override fun skip(count: Long): Long =
        super.skip(count).also { remaining -= it }

    override fun mark(readLimit: Int) {
        super.mark(readLimit)
        marked = remaining
    }

    override fun reset() {
        super.reset()
        remaining = marked
    }

}
