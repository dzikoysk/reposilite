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

package com.reposilite.status.application

import com.reposilite.plugin.api.PluginComponents
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.status.FailureFacade
import com.reposilite.status.StatusFacade
import com.reposilite.status.ThreadPoolCapacity
import panda.std.reactive.Reference
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

class StatusComponents(
    private val testEnv: Boolean,
    private val failureFacade: FailureFacade,
    private val remoteClientProvider: RemoteClientProvider,
    private val remoteVersionEndpoint: String,
    private val statusSupplier: () -> Boolean,
    private val threadPoolCapacity: () -> ThreadPoolCapacity,
    private val ioService: Executor
) : PluginComponents {

    constructor(
        testEnv: Boolean,
        failureFacade: FailureFacade,
        remoteClientProvider: RemoteClientProvider,
        remoteVersionEndpoint: String,
        statusSupplier: () -> Boolean,
        maxThreads: Reference<Int>
    ) : this(
        testEnv = testEnv,
        failureFacade = failureFacade,
        remoteClientProvider = remoteClientProvider,
        remoteVersionEndpoint = remoteVersionEndpoint,
        statusSupplier = statusSupplier,
        threadPoolCapacity = { ThreadPoolCapacity(used = Thread.activeCount(), max = maxThreads.get()) },
        ioService = ForkJoinPool.commonPool()
    )

    fun statusFacade(): StatusFacade =
        StatusFacade(
            testEnv = testEnv,
            status = statusSupplier,
            threadPoolCapacity = threadPoolCapacity,
            remoteVersionUrl = remoteVersionEndpoint,
            remoteClientProvider = remoteClientProvider,
            failureFacade = failureFacade,
            ioService = ioService
        )

}
