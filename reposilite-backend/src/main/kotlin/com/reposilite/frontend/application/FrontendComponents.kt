package com.reposilite.frontend.application

import com.reposilite.frontend.FrontendFacade
import com.reposilite.plugin.api.PluginComponents
import panda.std.reactive.Reference

class FrontendComponents(
    private val cacheContent: Reference<Boolean>,
    private val basePath: Reference<String>,
    private val frontendSettings: Reference<FrontendSettings>
) : PluginComponents {

    fun frontendFacade(): FrontendFacade =
        FrontendFacade(
            cacheContent = cacheContent,
            basePath = basePath,
            frontendSettings = frontendSettings
        )

}