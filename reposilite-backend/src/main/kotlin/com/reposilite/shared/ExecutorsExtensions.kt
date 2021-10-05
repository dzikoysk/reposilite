package com.reposilite.shared

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(private val prefix: String) : ThreadFactory {

    private val group: ThreadGroup = System.getSecurityManager()?.threadGroup ?: Thread.currentThread().threadGroup
    private val threadCount = AtomicInteger(0)

    override fun newThread(runnalbe: Runnable): Thread =
        Thread(group, runnalbe, "$prefix${threadCount.getAndIncrement()}", 0)

}

fun newFixedThreadPool(min: Int, max: Int, prefix: String): ExecutorService =
    ThreadPoolExecutor(
        min, max,
        0L, MILLISECONDS,
        LinkedBlockingQueue(),
        NamedThreadFactory("$prefix ({$max}) - ")
    )

fun newSingleThreadScheduledExecutor(prefix: String): ScheduledExecutorService =
     ScheduledThreadPoolExecutor(1, NamedThreadFactory("$prefix (1) - 1"))