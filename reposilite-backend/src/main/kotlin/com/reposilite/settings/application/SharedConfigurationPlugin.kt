package com.reposilite.settings.application

import com.reposilite.maven.RepositoryVisibility
import com.reposilite.maven.application.FSStorageProviderSettings
import com.reposilite.maven.application.RepositoriesSettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.application.S3StorageProviderSettings
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.SharedConfigurationFacade
import com.reposilite.settings.api.*
import com.reposilite.settings.infrastructure.SharedConfigurationEndpoints
import com.reposilite.shared.extensions.loadCommandBasedConfiguration
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "sharedconfig", dependencies = ["settings"])
class SharedConfigurationPlugin: ReposilitePlugin() {
    override fun initialize(): SharedConfigurationFacade {
        val sharedConfigurationFacade = SharedConfigurationFacade()
        val sharedConfiguration = facade<SettingsFacade>().sharedConfiguration
        with (sharedConfigurationFacade) {
            registerHandler(SettingsHandler.of(
                "advanced",
                AdvancedSettings::class.java,
                { sharedConfiguration.advanced.get() },
                { sharedConfiguration.advanced.update(it) }
            ))
            registerHandler(SettingsHandler.of("all", Settings::class.java, sharedConfiguration::getSettingsDTO, sharedConfiguration::updateFromSettingsDTO))
        }
        event { event: RoutingSetupEvent ->
            event.registerRoutes(SharedConfigurationEndpoints(sharedConfigurationFacade))
        }
        return sharedConfigurationFacade
    }
}

private fun SharedConfiguration.updateFromSettingsDTO(settings: Settings): Settings {
    repositories.update(RepositoriesSettings(settings.repositories))

    ldap.update(settings.ldap)
    return getSettingsDTO()
}

private fun SharedConfiguration.getSettingsDTO(): Settings = Settings(appearance = appearance.get(), advanced = advanced.get(), repositories = repositories.get().repositories, statistics = statistics.get(), ldap = ldap.get())
