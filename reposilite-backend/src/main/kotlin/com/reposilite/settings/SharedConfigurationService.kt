package com.reposilite.settings

import com.reposilite.journalist.Journalist
import com.reposilite.settings.application.SettingsWebConfiguration
import net.dzikoysk.cdn.CdnFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.time.Instant

internal class SharedConfigurationService(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    private val settingsRepository: SettingsRepository
) {

    internal val sharedConfiguration = SharedConfiguration()
    private var fileUpdateTime = Instant.ofEpochMilli(0)
    private var databaseUpdateTime = Instant.ofEpochMilli(0)

    fun verifySharedConfiguration() {
        val remoteUpdateTime = settingsRepository.findConfigurationUpdateDate(SettingsWebConfiguration.SHARED_CONFIGURATION_FILE)

        if (remoteUpdateTime?.isAfter(databaseUpdateTime) == true) {
            this.databaseUpdateTime = remoteUpdateTime
            loadSharedConfigurationFromDatabase()
        }
    }

    fun loadSharedConfiguration(): SharedConfiguration =
        sharedConfiguration.also {
            val sharedConfigurationFile = workingDirectory.resolve(SettingsWebConfiguration.SHARED_CONFIGURATION_FILE)
            this.fileUpdateTime = Files.getLastModifiedTime(sharedConfigurationFile).toInstant()
            this.databaseUpdateTime = settingsRepository.findConfigurationUpdateDate(SettingsWebConfiguration.SHARED_CONFIGURATION_FILE) ?: Instant.ofEpochMilli(0)

            if (databaseUpdateTime?.isBefore(fileUpdateTime) == true) {
                loadSharedConfigurationFromFile()
            } else try {
                loadSharedConfigurationFromDatabase()
            } catch (exception: Exception) {
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
            SettingsWebConfiguration.SHARED_CONFIGURATION_FILE,
            sharedConfiguration
        )

        CdnFactory.createStandard().render(sharedConfiguration)
            .takeIf { it != settingsRepository.findConfiguration(SettingsWebConfiguration.SHARED_CONFIGURATION_FILE) }
            ?.run { settingsRepository.saveConfiguration(SettingsWebConfiguration.SHARED_CONFIGURATION_FILE, this) }
    }

    private fun loadSharedConfigurationFromDatabase() {
        val sharedConfigurationFile = getSharedFile()
        val standard = CdnFactory.createStandard()

        journalist.logger.info("Loading shared configuration from database")
        standard.load({ settingsRepository.findConfiguration(SettingsWebConfiguration.SHARED_CONFIGURATION_FILE)!! }, sharedConfiguration)

        standard.render(sharedConfiguration)
            .takeIf { it != Files.readAllBytes(sharedConfigurationFile).toString() }
            ?.run { Files.write(sharedConfigurationFile, this.toByteArray(), WRITE, TRUNCATE_EXISTING) }
    }

    private fun getSharedFile(): Path =
        workingDirectory.resolve(SettingsWebConfiguration.SHARED_CONFIGURATION_FILE)

}