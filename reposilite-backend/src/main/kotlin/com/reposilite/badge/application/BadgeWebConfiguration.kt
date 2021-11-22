package com.reposilite.badge.application

import com.reposilite.Reposilite
import com.reposilite.badge.BadgeFacade
import com.reposilite.badge.infrastructure.BadgeEndpoints
import com.reposilite.maven.MavenFacade
import com.reposilite.settings.SettingsFacade
import com.reposilite.web.WebConfiguration
import com.reposilite.web.application.ReposiliteRoutes

internal object BadgeWebConfiguration : WebConfiguration {

    fun createFacade(settingsFacade: SettingsFacade, mavenFacade: MavenFacade): BadgeFacade =
        BadgeFacade(settingsFacade.sharedConfiguration.id, mavenFacade)

    override fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> = setOf(
        BadgeEndpoints(reposilite.badgeFacade)
    )

}