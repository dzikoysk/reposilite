package com.reposilite.settings.api

import com.reposilite.auth.application.AuthenticationSettings
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.statistics.application.StatisticsSettings
import com.reposilite.web.application.WebSettings
import java.io.Serializable

// TODO: Keep domain settings in list, so we could extend this through plugin api etc.?

@Doc(title = "all", description = "All settings")
data class Settings (
    @Doc(title = "Appearance", description = "Appearance settings")
    val appearance: FrontendSettings,
    @Doc(title = "Advanced", description = "Advanced settings")
    val advanced: WebSettings,
    @Doc(title = "Repositories", description = "List of Maven repositories.")
    val repositories: Map<String, RepositorySettings>,
    @Doc(title = "Statistics", description = "Statistics settings")
    val statistics: StatisticsSettings,
    @Doc(title = "Authentication", description = "Authentication settings")
    val authentication: AuthenticationSettings
) : Serializable
