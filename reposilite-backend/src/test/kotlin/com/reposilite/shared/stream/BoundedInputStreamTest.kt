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

@file:Suppress("FunctionName")

package com.reposilite.shared.stream

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException

internal class BoundedInputStreamTest {

    @Test
    fun `should pass through a stream that fits exactly within the limit`() {
        // given: a 100-byte payload and a 100-byte limit
        val payload = ByteArray(100) { 'a'.code.toByte() }
        val bounded = BoundedInputStream(ByteArrayInputStream(payload), maxBytes = 100)

        // when: the stream is fully consumed
        val consumed = bounded.readBytes()

        // then: every byte is delivered without an overflow
        assertThat(consumed).isEqualTo(payload)
    }

    @Test
    fun `should throw when one byte over the limit is read`() {
        // given: a 101-byte payload and a 100-byte limit
        val payload = ByteArray(101) { 'a'.code.toByte() }
        val bounded = BoundedInputStream(ByteArrayInputStream(payload), maxBytes = 100)

        // when / then: reading the entire stream surfaces the limit
        assertThatThrownBy { bounded.readBytes() }
            .isInstanceOf(BoundedInputStreamLimitExceededException::class.java)
            .hasMessageContaining("100")
    }

    @Test
    fun `should pass through a stream that fits exactly when consumed byte by byte`() {
        // given: a 5-byte payload and a 5-byte limit consumed via single-byte reads
        val payload = byteArrayOf(1, 2, 3, 4, 5)
        val bounded = BoundedInputStream(ByteArrayInputStream(payload), maxBytes = 5)

        // when: the stream is read one byte at a time
        val consumed = mutableListOf<Int>()
        while (true) {
            val b = bounded.read()
            if (b == -1) break
            consumed += b
        }

        // then: the byte-by-byte path delivers everything cleanly
        assertThat(consumed).isEqualTo(listOf(1, 2, 3, 4, 5))
    }

    @Test
    fun `should throw on the byte-by-byte read that pushes past the limit`() {
        // given: a 6-byte payload and a 5-byte limit consumed via single-byte reads
        val bounded = BoundedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5, 6)), maxBytes = 5)
        repeat(5) { bounded.read() }

        // when / then: the next read crosses the limit
        assertThatThrownBy { bounded.read() }
            .isInstanceOf(BoundedInputStreamLimitExceededException::class.java)
    }

    @Test
    fun `should support an empty stream within a zero limit`() {
        // given: an empty stream and a zero-byte limit
        val bounded = BoundedInputStream(ByteArrayInputStream(ByteArray(0)), maxBytes = 0)

        // when / then: reading immediately returns EOF without an overflow
        assertThat(bounded.read()).isEqualTo(-1)
    }

    @Test
    fun `should reject a non-empty stream when the limit is zero`() {
        // given: a single-byte stream and a zero-byte limit
        val bounded = BoundedInputStream(ByteArrayInputStream(byteArrayOf(1)), maxBytes = 0)

        // when / then: the very first byte trips the limit
        assertThatThrownBy { bounded.read() }
            .isInstanceOf(BoundedInputStreamLimitExceededException::class.java)
    }

    @Test
    fun `should enforce the limit when bytes are skipped`() {
        // given: a 200-byte payload and a 100-byte limit
        val bounded = BoundedInputStream(ByteArrayInputStream(ByteArray(200)), maxBytes = 100)

        // when / then: skipping past the limit must throw, not silently bypass
        assertThatThrownBy { bounded.skip(150) }
            .isInstanceOf(BoundedInputStreamLimitExceededException::class.java)
    }

    @Test
    fun `should allow skipping up to the limit`() {
        // given: a 100-byte payload and a 100-byte limit
        val bounded = BoundedInputStream(ByteArrayInputStream(ByteArray(100)), maxBytes = 100)

        // when: every byte is skipped
        val skipped = bounded.skip(100)

        // then: all 100 bytes are consumed without an overflow
        assertThat(skipped).isEqualTo(100L)
        assertThat(bounded.read()).isEqualTo(-1)
    }

    @Test
    fun `should not advertise mark support`() {
        val bounded = BoundedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), maxBytes = 10)

        assertThat(bounded.markSupported()).isFalse()
        assertThatThrownBy { bounded.reset() }.isInstanceOf(IOException::class.java)
    }

    @Test
    fun `should not over-consume the delegate when len greatly exceeds remaining headroom`() {
        // given: a delegate whose position we can observe and a 100-byte limit
        val payload = ByteArray(8192)
        val delegate = ByteArrayInputStream(payload)
        val bounded = BoundedInputStream(delegate, maxBytes = 100)

        // when: a single 8 KiB read is requested and trips the limit
        assertThatThrownBy { bounded.read(ByteArray(8192), 0, 8192) }
            .isInstanceOf(BoundedInputStreamLimitExceededException::class.java)

        // then: at most maxBytes + 1 bytes were pulled off the delegate (101 of 8192)
        assertThat(payload.size - delegate.available()).isLessThanOrEqualTo(101)
    }

    @Test
    fun `should treat read with len zero as a no-op`() {
        // given: a payload and a buffered read of length zero
        val bounded = BoundedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), maxBytes = 10)

        // when: a zero-length read is requested
        val n = bounded.read(ByteArray(8), 0, 0)

        // then: no bytes are reported and the stream is still readable afterwards
        assertThat(n).isEqualTo(0)
        assertThat(bounded.read()).isEqualTo(1)
    }

}
