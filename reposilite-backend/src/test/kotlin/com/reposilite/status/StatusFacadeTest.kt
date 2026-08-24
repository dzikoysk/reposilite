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

package com.reposilite.status

import com.reposilite.VERSION
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.shared.http.HttpRemoteClientProvider
import com.reposilite.status.api.InstanceStatusResponse
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

private const val REMOTE_VERSION_CHECK_PROPERTY = "reposilite.status.remote-version-check"

internal class StatusFacadeTest {

    private val executorSubmissions = AtomicInteger()
    private val ioService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "StatusFacadeTest | IO").apply { isDaemon = true }
    }
    private val trackedIoService = Executor { task ->
        executorSubmissions.incrementAndGet()
        ioService.execute(task)
    }

    @AfterEach
    fun shutdownIoService() {
        ioService.shutdownNow()
    }

    @Test
    fun `should return null without waiting for remote version check`() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { unavailableServer ->
            val statusFacade = createStatusFacade("http://127.0.0.1:${unavailableServer.localPort}")

            val status = assertTimeoutPreemptively<InstanceStatusResponse>(Duration.ofSeconds(1)) {
                statusFacade.fetchInstanceStatus()
            }

            assertThat(status.version).isEqualTo(VERSION)
            assertThat(status.latestVersion).isNull()
            assertThat(status.usedThreads).isEqualTo(3)
            assertThat(status.maxThreads).isEqualTo(16)
        }
    }

    @Test
    fun `should return remote version once available`() {
        val requests = AtomicInteger()
        val versionServer = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        versionServer.createContext("/") { exchange ->
            requests.incrementAndGet()
            VERSION.toByteArray().also { version ->
                exchange.sendResponseHeaders(200, version.size.toLong())
                exchange.responseBody.use { it.write(version) }
            }
        }
        versionServer.start()

        try {
            val statusFacade = createStatusFacade("http://127.0.0.1:${versionServer.address.port}")
            val status = assertTimeoutPreemptively<InstanceStatusResponse>(Duration.ofSeconds(1)) {
                generateSequence { statusFacade.fetchInstanceStatus() }
                    .first { it.latestVersion != null }
            }

            assertThat(status.latestVersion).isEqualTo(VERSION)
            assertThat(executorSubmissions).hasValue(1)
            repeat(10) { assertThat(statusFacade.getLatestVersion()).isEqualTo(VERSION) }
            assertThat(requests).hasValue(1)
        } finally {
            versionServer.stop(0)
        }
    }

    @Test
    fun `should return null when remote version check is disabled`() {
        System.setProperty(REMOTE_VERSION_CHECK_PROPERTY, "false")

        try {
            assertThat(createStatusFacade("https://example.com").fetchInstanceStatus().latestVersion).isNull()
        } finally {
            System.clearProperty(REMOTE_VERSION_CHECK_PROPERTY)
        }
    }

    @Test
    fun `should keep only the latest status snapshots`() {
        val statusFacade = createStatusFacade("https://example.com")

        repeat(20) { statusFacade.recordStatusSnapshot() }

        assertThat(statusFacade.getLatestStatusSnapshots()).hasSize(12)
    }

    private fun createStatusFacade(remoteVersionUrl: String): StatusFacade {
        val failureFacade = FailureFacade(InMemoryLogger())

        return StatusFacade(
            testEnv = false,
            threadPoolCapacity = { ThreadPoolCapacity(used = 3, max = 16) },
            status = { true },
            remoteVersionUrl = remoteVersionUrl,
            remoteClientProvider = HttpRemoteClientProvider(failureFacade),
            failureFacade = failureFacade,
            ioService = trackedIoService
        )
    }

}
