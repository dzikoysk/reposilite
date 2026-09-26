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

package com.reposilite.configuration.specification

import com.reposilite.configuration.application.ConfigurationComponents
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.configuration.shared.SharedConfigurationProvider
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.configuration.shared.application.SharedConfigurationComponents
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.plugin.Extensions
import com.reposilite.status.FailureFacade
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException

internal abstract class SharedReposiliteConfigurationSpecification {

    protected abstract val settings: Collection<SharedSettings>

    @TempDir
    lateinit var workingDirectory: File

    private val logger = PrintStreamLogger(System.out, System.err)
    protected lateinit var sharedConfigurationFacade: SharedConfigurationFacade
    protected val configurationProvider = TestConfigurationProvider()

    @BeforeEach
    fun prepare() {
        this.sharedConfigurationFacade = SharedConfigurationComponents(
            journalist = logger,
            workingDirectory = workingDirectory.toPath(),
            extensions = Extensions(logger),
            sharedConfigurationPath = Path.of("shared.json"),
            failureFacade = FailureFacade(logger),
            configurationFacade = ConfigurationComponents().configurationFacade()
        ).sharedConfigurationFacade(
            settings = settings,
            sharedConfigurationProvider = configurationProvider,
        )
    }

    protected fun useConcurrentUpdates(first: (() -> Unit) -> Unit, second: () -> Unit) {
        val executor = Executors.newFixedThreadPool(2)
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondStarted = CountDownLatch(1)

        try {
            val firstUpdate = executor.submit {
                first {
                    firstEntered.countDown()
                    check(releaseFirst.await(5, SECONDS)) { "First update was not released" }
                }
            }
            assertThat(firstEntered.await(5, SECONDS)).isTrue()
            val secondUpdate = executor.submit {
                secondStarted.countDown()
                second()
            }
            assertThat(secondStarted.await(5, SECONDS)).isTrue()
            assertThatThrownBy { secondUpdate.get(200, MILLISECONDS) }
                .isInstanceOf(TimeoutException::class.java)

            releaseFirst.countDown()
            firstUpdate.get(5, SECONDS)
            secondUpdate.get(5, SECONDS)
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
            check(executor.awaitTermination(5, SECONDS)) { "Settings updates did not finish" }
        }
    }

    protected class TestConfigurationProvider : SharedConfigurationProvider {

        var content = ""
        var updateRequired = false
        var onFetch: () -> Unit = {}
        var onUpdate: () -> Unit = {}

        override fun updateConfiguration(content: String) {
            onUpdate()
            this.content = content
            updateRequired = false
        }

        override fun fetchConfiguration(): String {
            val fetched = content
            onFetch()
            updateRequired = false
            return fetched
        }

        override fun isUpdateRequired(): Boolean =
            updateRequired

        override fun isMutable(): Boolean =
            true

        override fun name(): String =
            "test"

    }

}
