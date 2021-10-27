package com.reposilite.settings

import com.reposilite.journalist.Journalist
import com.reposilite.settings.application.SettingsWebConfiguration.SHARED_CONFIGURATION_FILE
import net.dzikoysk.cdn.CdnFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class SettingsFacade internal constructor(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    private val settingsRepository: SettingsRepository
) {

    private val sharedConfiguration = SharedConfiguration()

    fun loadSharedConfiguration(): SharedConfiguration =
        sharedConfiguration.also {
            val sharedConfigurationFile = workingDirectory.resolve(SHARED_CONFIGURATION_FILE)
            val fileUpdateTime = Files.getLastModifiedTime(sharedConfigurationFile).toInstant()
            val databaseUpdateTime = settingsRepository.findConfigurationUpdateDate(SHARED_CONFIGURATION_FILE)

            if (databaseUpdateTime == null || fileUpdateTime.isAfter(databaseUpdateTime)) {
                loadSharedConfigurationFromFile()
            }
            else try {
                loadSharedConfigurationFromDatabase()
            }
            catch (exception: Exception) {
                journalist.logger.error("Cannot load configuration from database")
                journalist.logger.exception(exception)
                journalist.logger.error("Restoring shared configuration from local filesystem")
                loadSharedConfigurationFromFile()
            }
        }

    private fun loadSharedConfigurationFromFile() {
        journalist.logger.info("Loading shared configuration from local file")

        SettingsFileLoader.initializeAndLoad(
            journalist,
            "copy",
            getSharedFile(),
            workingDirectory,
            SHARED_CONFIGURATION_FILE,
            sharedConfiguration
        )

        val output = CdnFactory.createStandard().render(sharedConfiguration)

        if (settingsRepository.findConfiguration(SHARED_CONFIGURATION_FILE) != output) {
            settingsRepository.saveConfiguration(SHARED_CONFIGURATION_FILE, output)
        }
    }

    private fun loadSharedConfigurationFromDatabase() {
        val sharedConfigurationFile = getSharedFile()
        val standard = CdnFactory.createStandard()

        journalist.logger.info("Loading shared configuration from database")
        standard.load({ settingsRepository.findConfiguration(SHARED_CONFIGURATION_FILE)!! }, sharedConfiguration)

        val output = standard.render(sharedConfiguration)

        if (Files.readAllBytes(sharedConfigurationFile).toString() != output) {
            Files.write(sharedConfigurationFile, output.toByteArray(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }

    private fun getSharedFile(): Path =
        workingDirectory.resolve(SHARED_CONFIGURATION_FILE)

}