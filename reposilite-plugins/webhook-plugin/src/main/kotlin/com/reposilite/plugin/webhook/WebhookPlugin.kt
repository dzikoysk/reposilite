package com.reposilite.plugin.webhook

import com.reposilite.maven.api.DeployEvent
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event

@Plugin(name = "webhook")
class WebhookPlugin : ReposilitePlugin() {
    override fun initialize(): Facade? {
        event { event: DeployEvent ->
            Webhook().fromEvent(event);
        }

        return null
    }
}