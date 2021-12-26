package com.reposilite.settings.specification

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.settings.ConfigurationProvider
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.settings.application.SettingsPlugin.Companion.SHARED_CONFIGURATION_FILE
import com.reposilite.settings.infrastructure.InMemorySettingsRepository
import com.reposilite.settings.infrastructure.SqlConfigurationProvider
import com.reposilite.web.http.ErrorResponse
import net.dzikoysk.cdn.KCdnFactory
import panda.std.Result

internal abstract class SettingsSpecification {

    private val localConfiguration = LocalConfiguration()
    protected val settingsRepository = InMemorySettingsRepository()
    protected val configurationProvider: ConfigurationProvider<SharedConfiguration> = SqlConfigurationProvider(
        "Shared Configuration",
        InMemoryLogger(),
        settingsRepository,
        SHARED_CONFIGURATION_FILE,
        SharedConfiguration()
    )
    protected val settingsFacade = SettingsFacade(localConfiguration, lazy { throw UnsupportedOperationException() }, configurationProvider)
    protected val cdn = KCdnFactory.createStandard()

    fun configurationFromRepository(): String? =
        settingsRepository.findConfiguration(SHARED_CONFIGURATION_FILE)

    fun configurationFromProvider(): Result<String, ErrorResponse> =
        settingsFacade.resolveConfiguration(SHARED_CONFIGURATION_FILE).map { it.content }

    fun renderConfiguration(): String =
        cdn.render(settingsFacade.sharedConfiguration).get()

}