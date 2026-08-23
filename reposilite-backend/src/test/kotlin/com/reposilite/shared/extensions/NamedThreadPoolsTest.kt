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

package com.reposilite.shared.extensions

import com.reposilite.Reposilite
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

internal class NamedThreadPoolsTest {

    @Test
    fun `should remove cancelled scheduled tasks`() {
        val scheduler = newSingleThreadScheduledExecutor("Test | Scheduler") as ScheduledThreadPoolExecutor

        try {
            val task = scheduler.schedule({}, 1, HOURS)
            task.cancel(false)

            assertThat(scheduler.queue).isEmpty()
        } finally {
            scheduler.shutdownNow()
        }
    }

    @Test
    fun `should grow to maximum size, queue excess work, and shrink when idle`() {
        val threadPool = newQueuedThreadPool(min = 0, max = 8, prefix = "Test | IO").apply {
            idleTimeout = 50
            maxEvictCount = 8
            reservedThreads = 0
            start()
        }
        val releaseFirstHalf = CountDownLatch(1)
        val releaseSecondHalf = CountDownLatch(1)
        val queuedTaskStarted = CountDownLatch(1)

        try {
            repeat(8) { workerIndex ->
                val workerStarted = CountDownLatch(1)
                threadPool.execute {
                    workerStarted.countDown()
                    when {
                        workerIndex < 4 -> releaseFirstHalf.await()
                        else -> releaseSecondHalf.await()
                    }
                }
                assertThat(workerStarted.await(2, SECONDS)).isTrue()
            }

            threadPool.execute { queuedTaskStarted.countDown() }
            assertThat(queuedTaskStarted.await(200, MILLISECONDS)).isFalse()

            releaseFirstHalf.countDown()
            assertThat(queuedTaskStarted.await(2, SECONDS)).isTrue()
            assertThat(waitUntil(2, SECONDS) { threadPool.threads == 4 })
                .withFailMessage("Expected four running workers, but pool was %s", threadPool)
                .isTrue()

            releaseSecondHalf.countDown()
            assertThat(waitUntil(2, SECONDS) { threadPool.threads == 0 }).isTrue()
        } finally {
            releaseFirstHalf.countDown()
            releaseSecondHalf.countDown()
            threadPool.stop()
        }
    }

    @Test
    fun `should reject a non-positive maximum size`() {
        assertThatThrownBy { newFixedThreadPool(min = 0, max = 0, prefix = "Test | Invalid") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Maximum thread pool size must be greater than 0")
    }

    @Test
    fun `should expose Jetty pool through executor service lifecycle`() {
        val executor = newFixedThreadPool(min = 0, max = 1, prefix = "Test | Lifecycle")
        val taskCompleted = CountDownLatch(1)

        executor.execute { taskCompleted.countDown() }
        assertThat(taskCompleted.await(2, SECONDS)).isTrue()

        executor.shutdown()
        assertThat(executor.isShutdown).isTrue()
        assertThat(executor.isTerminated).isTrue()
        assertThat(executor.asQueuedThreadPool()?.isStopped).isTrue()
    }

    @Test
    fun `should preserve executor service ABI`() {
        assertThat(Reposilite::class.java.getMethod("getIoService").returnType)
            .isEqualTo(ExecutorService::class.java)
    }

    private fun waitUntil(timeout: Long, unit: java.util.concurrent.TimeUnit, condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + unit.toNanos(timeout)
        while (System.nanoTime() < deadline) {
            if (condition()) {
                return true
            }
            Thread.sleep(10)
        }
        return condition()
    }

}
