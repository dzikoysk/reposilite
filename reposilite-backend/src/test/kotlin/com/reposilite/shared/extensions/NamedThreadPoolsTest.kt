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
import com.reposilite.web.validateWebThreadPoolSize
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
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
        val threadPool = newFixedThreadPool(min = 0, max = 4, prefix = "Test | IO") as ThreadPoolExecutor
        threadPool.setKeepAliveTime(50, MILLISECONDS)
        val workersStarted = CountDownLatch(4)
        val releaseWorkers = CountDownLatch(1)
        val queuedTaskStarted = CountDownLatch(1)

        try {
            repeat(4) {
                threadPool.execute {
                    workersStarted.countDown()
                    releaseWorkers.await()
                }
            }
            assertThat(workersStarted.await(2, SECONDS)).isTrue()

            threadPool.execute { queuedTaskStarted.countDown() }
            assertThat(threadPool.queue).hasSize(1)
            assertThat(queuedTaskStarted.await(200, MILLISECONDS)).isFalse()

            releaseWorkers.countDown()
            assertThat(queuedTaskStarted.await(2, SECONDS)).isTrue()
            assertThat(waitUntil(2, SECONDS) { threadPool.poolSize == 0 }).isTrue()
        } finally {
            releaseWorkers.countDown()
            threadPool.shutdownNow()
        }
    }

    @Test
    fun `should reject unsupported pool sizes`() {
        assertThatThrownBy { newFixedThreadPool(min = 0, max = 0, prefix = "Test | Invalid") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Maximum thread pool size must be greater than 0")
        assertThatThrownBy { newFixedThreadPool(min = 1, max = 1, prefix = "Test | Invalid") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Minimum thread pool size must be 0")

        validateWebThreadPoolSize(maxThreads = 4, sslEnabled = false)
        validateWebThreadPoolSize(maxThreads = 6, sslEnabled = true)
        assertThatThrownBy { validateWebThreadPoolSize(maxThreads = 3, sslEnabled = false) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Web thread pool size must be at least 4")
        assertThatThrownBy { validateWebThreadPoolSize(maxThreads = 5, sslEnabled = true) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Web thread pool size must be at least 6 when SSL is enabled")
    }

    @Test
    fun `should wait for work during graceful shutdown`() {
        val executor = newFixedThreadPool(min = 0, max = 1, prefix = "Test | Graceful Shutdown")
        val taskCompleted = CountDownLatch(1)

        executor.execute { taskCompleted.countDown() }
        assertThat(executor.shutdownGracefully(2, SECONDS)).isTrue()

        assertThat(taskCompleted.count).isZero()
        assertThat(executor.isTerminated).isTrue()
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
