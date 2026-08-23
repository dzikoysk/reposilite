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

package com.reposilite.shared.extensions

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.eclipse.jetty.util.thread.QueuedThreadPool

open class NamedThreadFactory(private val prefix: String) : ThreadFactory {

    private val group = Thread.currentThread().threadGroup
    private val threadCount = AtomicInteger(0)

    override fun newThread(runnalbe: Runnable): Thread =
        Thread(group, runnalbe, "$prefix${threadCount.getAndIncrement()}", 0)

}

fun newFixedThreadPool(min: Int, max: Int, prefix: String): ExecutorService =
    QueuedThreadPoolExecutorService(newQueuedThreadPool(min, max, prefix).apply {
        reservedThreads = 0
        start()
    })

fun newSingleThreadScheduledExecutor(prefix: String): ScheduledExecutorService =
    ScheduledThreadPoolExecutor(1, NamedThreadFactory("$prefix (1) - ")).apply {
        removeOnCancelPolicy = true
        executeExistingDelayedTasksAfterShutdownPolicy = false
    }

internal fun newQueuedThreadPool(min: Int, max: Int, prefix: String): QueuedThreadPool {
    require(max > 0) { "Maximum thread pool size must be greater than 0" }
    require(min in 0..max) { "Minimum thread pool size must be between 0 and $max" }

    return QueuedThreadPool(max, min, 60_000).apply {
        name = "$prefix ($max)"
    }
}

internal fun ExecutorService.asQueuedThreadPool(): QueuedThreadPool? =
    (this as? QueuedThreadPoolExecutorService)?.threadPool

private class QueuedThreadPoolExecutorService(
    val threadPool: QueuedThreadPool
) : AbstractExecutorService() {

    private val shutdown = AtomicBoolean(false)
    private val terminated = CountDownLatch(1)

    override fun execute(command: Runnable) {
        if (isShutdown) {
            throw RejectedExecutionException(command.toString())
        }
        threadPool.execute(command)
    }

    override fun shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            try {
                threadPool.stop()
            } finally {
                terminated.countDown()
            }
        }
    }

    override fun shutdownNow(): MutableList<Runnable> {
        shutdown()
        return mutableListOf()
    }

    override fun isShutdown(): Boolean =
        shutdown.get()

    override fun isTerminated(): Boolean =
        terminated.count == 0L && !threadPool.isRunning

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        terminated.await(timeout, unit)
        return isTerminated
    }

}
