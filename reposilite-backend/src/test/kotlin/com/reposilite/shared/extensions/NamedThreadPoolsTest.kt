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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

internal class NamedThreadPoolsTest {

    @Test
    fun `should start configured number of workers`() {
        val threadPool = newFixedThreadPool(min = 0, max = 4, prefix = "Test | IO")
        val workersStarted = CountDownLatch(4)
        val releaseWorkers = CountDownLatch(1)

        try {
            repeat(4) {
                threadPool.execute {
                    workersStarted.countDown()
                    releaseWorkers.await()
                }
            }

            assertThat(workersStarted.await(2, SECONDS)).isTrue()
        } finally {
            releaseWorkers.countDown()
            threadPool.shutdown()
            threadPool.awaitTermination(2, SECONDS)
        }
    }

}
