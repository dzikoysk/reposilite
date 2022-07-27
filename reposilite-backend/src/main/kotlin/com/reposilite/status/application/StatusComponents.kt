package com.reposilite.status.application

import com.reposilite.plugin.api.PluginComponents
import com.reposilite.status.StatusFacade

class StatusComponents(
    private val testEnv: Boolean,
    private val remoteVersionEndpoint: String,
    private val statusSupplier: () -> Boolean
) : PluginComponents {

    fun statusFacade(): StatusFacade =
        StatusFacade(
            testEnv = testEnv,
            status = statusSupplier,
            remoteVersionUrl = remoteVersionEndpoint
        )

}