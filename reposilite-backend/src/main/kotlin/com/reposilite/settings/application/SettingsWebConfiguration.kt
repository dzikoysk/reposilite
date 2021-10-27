package com.reposilite.settings.application

import com.reposilite.Reposilite
import com.reposilite.ReposiliteParameters
import com.reposilite.journalist.Journalist
import com.reposilite.settings.LocalConfiguration
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.SettingsFileLoader
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
        SettingsFileLoader.initializeAndLoad(
            journalist,
            parameters.configurationMode,
            parameters.configurationFile,
            parameters.workingDirectory,
            LOCAL_CONFIGURATION_FILE,
            LocalConfiguration()
        )

    fun createFacade(journalist: Journalist, parameters: ReposiliteParameters, database: Database): SettingsFacade =
        SettingsFacade(
            journalist,
            SharedConfigurationService(journalist, parameters.workingDirectory, SqlSettingsRepository(database))
        )

    override fun initialize(reposilite: Reposilite) {
        reposilite.scheduler.scheduleWithFixedDelay({
            reposilite.settingsFacade.verifySharedConfiguration()
        }, 10, 10, TimeUnit.SECONDS)
    }

    override fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> = setOf(
        SettingsEndpoints()
    )

}