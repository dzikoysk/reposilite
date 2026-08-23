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

@file:Suppress("UnstableApiUsage")

package com.reposilite.status

import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.EvictingQueue
import com.reposilite.VERSION
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.status.api.InstanceStatusResponse
import com.reposilite.status.api.StatusSnapshot
import panda.std.reactive.Reference
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class ThreadPoolCapacity(
    val used: Int,
    val max: Int
)

class StatusFacade(
    private val testEnv: Boolean,
    private val startTime: Long = System.currentTimeMillis(),
    private val threadPoolCapacity: () -> ThreadPoolCapacity,
    private val status: () -> Boolean,
    private val remoteVersionUrl: String,
    private val remoteClientProvider: RemoteClientProvider,
    private val failureFacade: FailureFacade,
    private val ioService: Executor
) : Facade {

    constructor(
        testEnv: Boolean,
        startTime: Long = System.currentTimeMillis(),
        maxThreads: Reference<Int>,
        status: () -> Boolean,
        remoteVersionUrl: String,
        remoteClientProvider: RemoteClientProvider,
        failureFacade: FailureFacade
    ) : this(
        testEnv = testEnv,
        startTime = startTime,
        threadPoolCapacity = { ThreadPoolCapacity(used = Thread.activeCount(), max = maxThreads.get()) },
        status = status,
        remoteVersionUrl = remoteVersionUrl,
        remoteClientProvider = remoteClientProvider,
        failureFacade = failureFacade,
        ioService = ForkJoinPool.commonPool()
    )

    private val cachedStatusSnapshots = EvictingQueue.create<StatusSnapshot>(12)

    private val cachedLatestVersion: Supplier<CompletableFuture<String?>> =
        Suppliers.memoizeWithExpiration({
            when {
                testEnv ->
                    CompletableFuture.completedFuture(VERSION)
                System.getProperty("reposilite.status.remote-version-check", "true") != "true" ->
                    CompletableFuture.completedFuture(null)
                else ->
                    CompletableFuture.supplyAsync({
                        remoteClientProvider
                            .defaultClient
                            .get(remoteVersionUrl, null, 3, 15)
                            .onError {
                                when (it.message.contains("java.security.NoSuchAlgorithmException")) {
                                    true -> failureFacade.logger.warn("Cannot load SSL context for HTTPS request due to the lack of available memory")
                                    else -> failureFacade.logger.warn("$remoteVersionUrl is unavailable: ${it.message}")
                                }
                            }
                            .map { stream -> stream.bufferedReader().use { it.readText() } }
                            .orNull()
                    }, ioService)
            }
        }, 1, TimeUnit.HOURS)

    init {
        recordStatusSnapshot()
    }

    fun recordStatusSnapshot() {
        synchronized(cachedStatusSnapshots) {
            cachedStatusSnapshots.add(
                StatusSnapshot(
                    memory = getUsedMemory().roundToInt(),
                    threads = threadPoolCapacity().used
                )
            )
        }
    }

    fun fetchInstanceStatus(): InstanceStatusResponse =
        threadPoolCapacity().let { threads ->
            InstanceStatusResponse(
                version = VERSION,
                latestVersion = cachedLatestVersion.get().getNow(null),
                uptime = System.currentTimeMillis() - getUptime(),
                usedMemory = getUsedMemory(),
                maxMemory = (Runtime.getRuntime().maxMemory() / 1024 / 1024).toInt(),
                usedThreads = threads.used,
                maxThreads = threads.max,
                failuresCount = failureFacade.getFailuresCount()
            )
        }

    private fun getUsedMemory(): Double =
        (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0

    internal fun getLatestVersion(): String? =
        cachedLatestVersion
            .get()
            .getNow(null)

    fun getLatestStatusSnapshots(): Array<StatusSnapshot> =
        synchronized(cachedStatusSnapshots) {
            cachedStatusSnapshots.toTypedArray()
        }

    fun getUptime(): Long =
        startTime

    fun isAlive(): Boolean =
        status()

}
