/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.status

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.reposilite.VERSION
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.status.api.InstanceStatusResponse
import com.reposilite.status.api.StatusSnapshot
import panda.std.reactive.Reference
import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.HOURS
import kotlin.math.roundToInt

private const val MAX_STATUS_SNAPSHOTS = 12

class StatusFacade(
    private val testEnv: Boolean,
    private val startTime: Long = System.currentTimeMillis(),
    private val maxThreads: Reference<Int>,
    private val status: () -> Boolean,
    private val remoteVersionUrl: String,
    private val remoteClientProvider: RemoteClientProvider,
    private val failureFacade: FailureFacade
) : Facade {

    private val cachedStatusSnapshots = ArrayDeque<StatusSnapshot>(MAX_STATUS_SNAPSHOTS)

    private val cachedLatestVersion: Cache<Unit, CompletableFuture<String?>> =
        Caffeine
            .newBuilder()
            .maximumSize(1)
            .expireAfterWrite(1, HOURS)
            .build()

    init {
        recordStatusSnapshot()
    }

    fun recordStatusSnapshot() {
        if (cachedStatusSnapshots.size == MAX_STATUS_SNAPSHOTS) {
            cachedStatusSnapshots.removeFirst()
        }

        cachedStatusSnapshots.addLast(
            StatusSnapshot(
                memory = getUsedMemory().roundToInt(),
                threads = getUsedThreads()
            )
        )
    }

    fun fetchInstanceStatus(): InstanceStatusResponse =
        InstanceStatusResponse(
            version = VERSION,
            latestVersion = getLatestVersion(),
            uptime = System.currentTimeMillis() - getUptime(),
            usedMemory = getUsedMemory(),
            maxMemory = (Runtime.getRuntime().maxMemory() / 1024 / 1024).toInt(),
            usedThreads = getUsedThreads(),
            maxThreads = maxThreads.get(),
            failuresCount = failureFacade.getFailuresCount()
        )

    private fun getUsedThreads(): Int =
        Thread.activeCount()

    private fun getUsedMemory(): Double =
        (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0

    internal fun getLatestVersion(): String? =
        cachedLatestVersion
            .get(Unit) { fetchLatestVersion() }
            .getNow(null)

    private fun fetchLatestVersion(): CompletableFuture<String?> =
        when {
            testEnv ->
                CompletableFuture.completedFuture(VERSION)
            System.getProperty("reposilite.status.remote-version-check", "true") != "true" ->
                CompletableFuture.completedFuture(null)
            else ->
                CompletableFuture.supplyAsync {
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
                }
        }

    fun getLatestStatusSnapshots(): Array<StatusSnapshot> =
        cachedStatusSnapshots.toTypedArray()

    fun getUptime(): Long =
        startTime

    fun isAlive(): Boolean =
        status()

}
