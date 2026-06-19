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

package com.reposilite.storage

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.status.FailureFacade
import com.reposilite.storage.api.toLocation
import com.reposilite.storage.filesystem.FileSystemStorageProvider
import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal class FileSystemStorageProviderIntegrationTest : StorageProviderIntegrationTest() {

    @TempDir
    lateinit var rootDirectory: File

    @BeforeEach
    fun setup() {
        val logger = InMemoryLogger()
        val failureFacade = FailureFacade(logger)
        val storageFacade = StorageFacade()

        super.storageProvider = storageFacade.createStorageProvider(
            journalist = logger,
            failureFacade = failureFacade,
            workingDirectory = rootDirectory.toPath(),
            repository = "test-storage",
            storageSettings = FileSystemStorageProviderSettings(quota = "1MB")
        )!!
    }

    @Nested
    inner class Concurrency {

        @Test
        fun `should keep read-write exclusion when input stream is closed multiple times`() {
            // given: an existing file whose input stream is closed twice (frameworks may close defensively)
            val location = "/dir/double-close.jar".toLocation()
            storageProvider.putFile(location, "content".byteInputStream())
            val redundantlyClosed = assertOk(storageProvider.getFile(location))
            redundantlyClosed.close()
            redundantlyClosed.close()

            // and: a fresh reader is opened after the double-close
            val activeReader = assertOk(storageProvider.getFile(location))

            // when: a writer attempts to overwrite the file in parallel
            val writerLatch = CountDownLatch(1)
            val writerThread = thread(start = true, isDaemon = true) {
                storageProvider.putFile(location, "rewrite".byteInputStream())
                writerLatch.countDown()
            }

            try {
                // then: the writer parks on the lock instead of slipping through with a corrupted counter
                awaitTerminalOrParked(writerThread)
                assertThat(writerLatch.count).isEqualTo(1L)
            } finally {
                activeReader.close()
                writerThread.join(SAFETY_TIMEOUT_MS)
            }
        }

        @Test
        fun `should serialize a writer behind multiple concurrent readers`() {
            // given: an existing file with two concurrently-open readers
            val location = "/dir/multi-reader.jar".toLocation()
            storageProvider.putFile(location, "content".byteInputStream())
            val firstReader = assertOk(storageProvider.getFile(location))
            val secondReader = assertOk(storageProvider.getFile(location))

            // when: a writer attempts to overwrite the file in parallel
            val writerLatch = CountDownLatch(1)
            val writerThread = thread(start = true, isDaemon = true) {
                storageProvider.putFile(location, "rewrite".byteInputStream())
                writerLatch.countDown()
            }
            awaitTerminalOrParked(writerThread)

            // then: the writer is still blocked while a second reader is open
            firstReader.close()
            awaitTerminalOrParked(writerThread)
            assertThat(writerLatch.count).isEqualTo(1L)

            // and: the writer proceeds once the last reader releases
            secondReader.close()
            assertThat(writerLatch.await(SAFETY_TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue
        }

        @Test
        fun `should evict the per-location accessor once every acquire is released`() {
            // given: a file system storage provider with no in-flight acquires
            val provider = storageProvider as FileSystemStorageProvider
            val location = "/dir/eviction.jar".toLocation()
            provider.putFile(location, "content".byteInputStream())
            assertThat(provider.lockedLocationsSize()).isZero

            // when: many concurrent reads come and go on the same location
            val threads = (1..20).map {
                thread(start = true, isDaemon = true) {
                    repeat(10) {
                        assertOk(provider.getFile(location)).close()
                    }
                }
            }
            threads.forEach { it.join(SAFETY_TIMEOUT_MS) }

            // then: the lockedLocations map is empty — no leaked accessors
            assertThat(provider.lockedLocationsSize()).isZero
        }

        @Test
        fun `should reject upload exceeding quota when the stream reports zero available bytes`() {
            // given: a payload larger than the configured 1 MB quota whose available() always returns 0
            val payload = ByteArray(2 * 1024 * 1024) { 'a'.code.toByte() }
            val streamReportingZero = object : InputStream() {
                private val delegate = payload.inputStream()
                override fun read(): Int = delegate.read()
                override fun available(): Int = 0
            }

            // when: the upload is attempted
            val response = storageProvider.putFile("/oversized.jar".toLocation(), streamReportingZero)

            // then: the storage provider rejects it
            assertError(response)
        }

        @Test
        fun `should accept an upload exactly at the quota boundary`() {
            // given: a payload of exactly the configured 1 MB
            val payload = ByteArray(1024 * 1024) { 'a'.code.toByte() }

            // when: the upload is attempted
            val response = storageProvider.putFile("/exact.jar".toLocation(), payload.inputStream())

            // then: the storage provider accepts it cleanly
            assertOk(response)
        }

        @Test
        fun `should reject an upload meaningfully over the quota boundary`() {
            // given: a payload comfortably above the configured 1 MB
            val payload = ByteArray(2 * 1024 * 1024) { 'a'.code.toByte() }

            // when: the upload is attempted
            val response = storageProvider.putFile("/over.jar".toLocation(), payload.inputStream())

            // then: the storage provider rejects it
            assertError(response)
        }

        // Block until the thread is parked on a lock or has terminated, so subsequent assertions
        // observe a stable state instead of guessing with a fixed sleep. The deadline is just a
        // safety net to avoid an indefinite hang if the production code regresses.
        private fun awaitTerminalOrParked(thread: Thread) {
            val deadline = System.nanoTime() + SAFETY_TIMEOUT_MS * 1_000_000L
            while (System.nanoTime() < deadline) {
                when (thread.state) {
                    Thread.State.WAITING, Thread.State.TIMED_WAITING, Thread.State.TERMINATED -> return
                    else -> Thread.onSpinWait()
                }
            }
            error("Thread did not park or terminate, last state was ${thread.state}")
        }

    }

    private companion object {
        private const val SAFETY_TIMEOUT_MS = 5_000L
    }

}
