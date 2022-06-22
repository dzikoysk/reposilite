package com.reposilite.plugin.webhook

import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin

@Plugin(name = "webhook")
class WebhookPlugin : ReposilitePlugin() {
    override fun initialize(): Facade? {
        return null
    }
}