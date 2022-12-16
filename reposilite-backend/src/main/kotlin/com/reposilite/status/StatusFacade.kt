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

@file:Suppress("UnstableApiUsage")

package com.reposilite.status

import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.EvictingQueue
import com.reposilite.VERSION
import com.reposilite.plugin.api.Facade
import com.reposilite.status.api.InstanceStatusResponse
import com.reposilite.status.api.StatusSnapshot
import panda.std.Result
import panda.std.reactive.Reference
import panda.utilities.IOUtils
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class StatusFacade(
    private val testEnv: Boolean,
    private val startTime: Long = System.currentTimeMillis(),
    private val maxMemory: Long = Runtime.getRuntime().totalMemory(),
    private val maxThreads: Reference<Int>,
    private val status: () -> Boolean,
    private val remoteVersionUrl: String,
    private val failureFacade: FailureFacade
) : Facade {

    private val cachedLatestVersion: Supplier<Result<String, String>> = Suppliers.memoizeWithExpiration({
        when {
            testEnv ->
                Result.ok(VERSION)
            else ->
                IOUtils.fetchContent(remoteVersionUrl)
                    .onError {
                        when (it.message?.contains("java.security.NoSuchAlgorithmException")) {
                            true -> failureFacade.logger.warn("Cannot load SSL context for HTTPS request due to the lack of available memory")
                            else -> failureFacade.logger.warn("$remoteVersionUrl is unavailable: ${it.message}")
                        }
                    }
                    .mapErr { "<unknown>" }
        }
    }, 1, TimeUnit.HOURS)

    private val cachedStatusSnapshots = EvictingQueue.create<StatusSnapshot>(12)

    init {
        recordStatusSnapshot()
    }

    fun recordStatusSnapshot() {
        cachedStatusSnapshots.add(
            StatusSnapshot(
                memory = getUsedMemory().roundToInt(),
                threads = getUsedThreads()
            )
        )
    }

    fun fetchInstanceStatus(): InstanceStatusResponse =
        InstanceStatusResponse(
            version = VERSION,
            latestVersion = cachedLatestVersion.get().fold({ it }, { it }),
            uptime = System.currentTimeMillis() - getUptime(),
            usedMemory = getUsedMemory(),
            maxMemory = (maxMemory / 1024 / 1024).toInt(),
            usedThreads = getUsedThreads(),
            maxThreads = maxThreads.get(),
            failuresCount = failureFacade.getFailures().size
        )

    private fun getUsedThreads(): Int =
        Thread.activeCount()

    private fun getUsedMemory(): Double =
        (maxMemory - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0

    internal fun getLatestVersion(): Result<String, String> =
        cachedLatestVersion.get()

    fun getLatestStatusSnapshots(): Array<StatusSnapshot> =
        cachedStatusSnapshots.toTypedArray()

    fun getUptime(): Long =
        startTime

    fun isAlive(): Boolean =
        status()

}
