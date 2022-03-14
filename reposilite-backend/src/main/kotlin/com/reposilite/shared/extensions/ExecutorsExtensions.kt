/*
 * Copyright (c) 2022 dzikoysk
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

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger

internal class NamedThreadFactory(private val prefix: String) : ThreadFactory {

    private val group = Thread.currentThread().threadGroup
    private val threadCount = AtomicInteger(0)

    override fun newThread(runnalbe: Runnable): Thread =
        Thread(group, runnalbe, "$prefix${threadCount.getAndIncrement()}", 0)

}

internal fun newFixedThreadPool(min: Int, max: Int, prefix: String): ExecutorService =
    ThreadPoolExecutor(
        min, max,
        0L, MILLISECONDS,
        LinkedBlockingQueue(),
        NamedThreadFactory("$prefix ($max) - ")
    )

internal fun newSingleThreadScheduledExecutor(prefix: String): ScheduledExecutorService =
     ScheduledThreadPoolExecutor(1, NamedThreadFactory("$prefix (1) - "))
