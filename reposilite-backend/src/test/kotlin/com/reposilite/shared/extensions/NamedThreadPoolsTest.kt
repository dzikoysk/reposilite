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
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean

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
    fun `should reject a non-positive maximum size`() {
        assertThatThrownBy { newFixedThreadPool(min = 0, max = 0, prefix = "Test | Invalid") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Maximum thread pool size must be greater than 0")
    }

    @Test
    fun `should reject a non-zero minimum size`() {
        assertThatThrownBy { newFixedThreadPool(min = 1, max = 1, prefix = "Test | Invalid") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Minimum thread pool size must be 0")
    }

    @Test
    fun `should complete running and queued tasks during orderly shutdown`() {
        val executor = newFixedThreadPool(min = 0, max = 1, prefix = "Test | Orderly Shutdown")
        val runningTaskStarted = CountDownLatch(1)
        val releaseRunningTask = CountDownLatch(1)
        val queuedTaskCompleted = CountDownLatch(1)

        val runningTask = executor.submit {
            runningTaskStarted.countDown()
            releaseRunningTask.await()
        }
        val queuedTask = executor.submit { queuedTaskCompleted.countDown() }
        assertThat(runningTaskStarted.await(2, SECONDS)).isTrue()

        executor.shutdown()
        assertThat(executor.isShutdown).isTrue()
        assertThat(executor.isTerminated).isFalse()
        assertThat(queuedTaskCompleted.await(200, MILLISECONDS)).isFalse()

        releaseRunningTask.countDown()
        assertThat(executor.awaitTermination(2, SECONDS)).isTrue()
        assertThat(runningTask).isDone()
        assertThat(queuedTask).isDone()

        assertThatThrownBy { executor.execute {} }
            .isInstanceOf(RejectedExecutionException::class.java)
    }

    @Test
    fun `should interrupt running work and return queued work during immediate shutdown`() {
        val executor = newFixedThreadPool(min = 0, max = 1, prefix = "Test | Immediate Shutdown")
        val runningTaskStarted = CountDownLatch(1)
        val runningTaskInterrupted = AtomicBoolean(false)
        val queuedTask = Runnable {}

        executor.execute {
            runningTaskStarted.countDown()
            try {
                CountDownLatch(1).await()
            } catch (_: InterruptedException) {
                runningTaskInterrupted.set(true)
            }
        }
        assertThat(runningTaskStarted.await(2, SECONDS)).isTrue()
        executor.execute(queuedTask)

        assertThat(executor.shutdownNow()).containsExactly(queuedTask)
        assertThat(executor.awaitTermination(2, SECONDS)).isTrue()
        assertThat(runningTaskInterrupted).isTrue()
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
