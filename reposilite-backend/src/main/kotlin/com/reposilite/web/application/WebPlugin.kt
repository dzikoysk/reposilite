package com.reposilite.web.application

import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin

@Plugin(name = "web", settings = WebSettings::class)
class WebPlugin : ReposilitePlugin() {

    override fun initialize(): Facade? =
        null

}