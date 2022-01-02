package com.reposilite.settings.specification

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.settings.application.SettingsPlugin.Companion.SHARED_CONFIGURATION_FILE
import com.reposilite.settings.infrastructure.InMemorySettingsRepository
import com.reposilite.web.http.ErrorResponse
import net.dzikoysk.cdn.KCdnFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import panda.std.Result
import java.io.File

internal abstract class SettingsSpecification {

    @TempDir
    protected lateinit var workingDirectory: File

    private val localConfiguration = LocalConfiguration()
    protected val settingsRepository = InMemorySettingsRepository()
    protected lateinit var settingsFacade: SettingsFacade
    protected val cdn = KCdnFactory.createStandard()

    @BeforeEach
    fun prepare() {
        this.settingsFacade = SettingsFacade(
            InMemoryLogger(),
            workingDirectory.toPath(),
            localConfiguration,
            lazy { throw UnsupportedOperationException() },
            settingsRepository
        )

        settingsFacade.createConfigurationProvider(SharedConfiguration(), "Shared configuration", SHARED_CONFIGURATION_FILE)
    }

    fun configurationFromRepository(): String? =
        settingsRepository.findConfiguration(SHARED_CONFIGURATION_FILE)

    fun configurationFromProvider(): Result<String, ErrorResponse> =
        settingsFacade.resolveConfiguration(SHARED_CONFIGURATION_FILE).map { it.content }

    fun renderConfiguration(): String =
        cdn.render(settingsFacade.sharedConfiguration).get()

}