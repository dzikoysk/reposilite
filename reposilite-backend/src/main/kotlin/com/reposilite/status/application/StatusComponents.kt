package com.reposilite.status.application

import com.reposilite.plugin.api.PluginComponents
import com.reposilite.status.FailureFacade
import com.reposilite.status.StatusFacade
import panda.std.reactive.Reference

class StatusComponents(
    private val testEnv: Boolean,
    private val failureFacade: FailureFacade,
    private val remoteVersionEndpoint: String,
    private val statusSupplier: () -> Boolean,
    private val maxThreads: Reference<Int>
) : PluginComponents {

    fun statusFacade(): StatusFacade =
        StatusFacade(
            testEnv = testEnv,
            status = statusSupplier,
            remoteVersionUrl = remoteVersionEndpoint,
            failureFacade = failureFacade,
            maxThreads = maxThreads
        )

}
