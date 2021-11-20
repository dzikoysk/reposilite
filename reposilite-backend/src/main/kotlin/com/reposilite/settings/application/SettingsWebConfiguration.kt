package com.reposilite.settings.application

import com.reposilite.Reposilite
import com.reposilite.ReposiliteParameters
import com.reposilite.journalist.Journalist
import com.reposilite.settings.LocalConfiguration
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.SharedConfigurationService
import com.reposilite.settings.infrastructure.SettingsEndpoints
import com.reposilite.settings.infrastructure.SqlSettingsRepository
import com.reposilite.web.WebConfiguration
import com.reposilite.web.application.ReposiliteRoutes
import org.jetbrains.exposed.sql.Database
import java.util.concurrent.TimeUnit

internal object SettingsWebConfiguration : WebConfiguration {

    const val LOCAL_CONFIGURATION_FILE = "configuration.local.cdn"
    const val SHARED_CONFIGURATION_FILE = "configuration.shared.cdn"

    fun createLocalConfiguration(journalist: Journalist, parameters: ReposiliteParameters): LocalConfiguration =
        SettingsFacade.createLocalConfiguration(journalist, parameters)

    fun createFacade(journalist: Journalist, parameters: ReposiliteParameters, localConfiguration: LocalConfiguration, database: Database): SettingsFacade {
        val sharedConfigurationService = SharedConfigurationService(
            journalist = journalist,
            settingsRepository = SqlSettingsRepository(database),
            workingDirectory = parameters.workingDirectory,
            sharedConfigurationMode = parameters.sharedConfigurationMode
        ).also { it.loadSharedConfiguration() }

        return SettingsFacade(localConfiguration, sharedConfigurationService)
    }

    override fun initialize(reposilite: Reposilite) {
        reposilite.scheduler.scheduleWithFixedDelay({
            reposilite.settingsFacade.synchronizeConfigurations()
        }, 10, 10, TimeUnit.SECONDS)
    }

    override fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> = setOf(
        SettingsEndpoints(reposilite.settingsFacade )
    )

}