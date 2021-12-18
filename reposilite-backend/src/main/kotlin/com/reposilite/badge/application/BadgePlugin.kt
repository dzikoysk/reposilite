package com.reposilite.badge.application

import com.reposilite.badge.BadgeFacade
import com.reposilite.badge.infrastructure.BadgeEndpoints
import com.reposilite.maven.MavenFacade
import com.reposilite.plugin.ReposilitePlugin
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.settings.SettingsFacade
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "badge", dependencies = ["configuration", "maven"])
class BadgePlugin : ReposilitePlugin() {

    override fun initialize(): Facade {
        val settingsFacade = facade<SettingsFacade>()
        val mavenFacade = facade<MavenFacade>()
        val badgeFacade = BadgeFacade(settingsFacade.sharedConfiguration.id, mavenFacade)

        extensions.registerEvent(RoutingSetupEvent::class.java) {
            it.registerRoutes(BadgeEndpoints(badgeFacade))
        }

        return badgeFacade
    }

}