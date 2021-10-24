package com.reposilite.settings

import com.reposilite.journalist.Journalist
import com.reposilite.settings.application.SettingsWebConfiguration.SHARED_CONFIGURATION_FILE
import net.dzikoysk.cdn.CdnFactory
import java.nio.file.Path

class SettingsFacade internal constructor(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    private val settingsRepository: SettingsRepository
) {

    private val sharedConfiguration = SharedConfiguration()

    fun loadSharedConfiguration(): SharedConfiguration =
        sharedConfiguration.also {
            SettingsFileLoader.initializeAndLoad(
                journalist,
                "copy",
                workingDirectory.resolve(SHARED_CONFIGURATION_FILE),
                workingDirectory,
                SHARED_CONFIGURATION_FILE,
                sharedConfiguration
            )

            CdnFactory.createStandard().load({ settingsRepository.findSharedConfiguration() }, sharedConfiguration)
        }

}