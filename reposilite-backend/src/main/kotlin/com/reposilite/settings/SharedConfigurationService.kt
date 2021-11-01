package com.reposilite.settings

import com.reposilite.journalist.Journalist
import com.reposilite.settings.SettingsFileLoader.validateAndLoad
import com.reposilite.settings.api.SettingsResponse
import com.reposilite.settings.api.SettingsUpdateRequest
import com.reposilite.settings.application.SettingsWebConfiguration.SHARED_CONFIGURATION_FILE
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFoundError
import io.javalin.http.ContentType.APPLICATION_CDN
import io.javalin.http.ContentType.APPLICATION_JSON
import io.javalin.http.ContentType.APPLICATION_YAML
import net.dzikoysk.cdn.Cdn
import net.dzikoysk.cdn.CdnFactory
import net.dzikoysk.cdn.source.Source
import panda.std.Result
import panda.std.asSuccess
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.time.Instant

internal class SharedConfigurationService(
    private val journalist: Journalist,
    private val settingsRepository: SettingsRepository,
    private val workingDirectory: Path,
    private val sharedFile: Path = workingDirectory.resolve(SHARED_CONFIGURATION_FILE),
    private val standard: Cdn = CdnFactory.createStandard(),
    internal val sharedConfiguration: SharedConfiguration = SharedConfiguration()
) {

    private var databaseUpdateTime = Instant.ofEpochMilli(0)

    fun synchronizeSharedConfiguration() =
        settingsRepository.findConfigurationUpdateDate(SHARED_CONFIGURATION_FILE)
            ?.takeIf { it.isAfter(databaseUpdateTime) }
            ?.run {
                journalist.logger.info("Propagation | Shared configuration has been changed in remote source, updating current instance...")
                databaseUpdateTime = this
                loadAndUpdate(fromFile = false)
            }

    fun resolveConfiguration(name: String): Result<SettingsResponse, ErrorResponse> =
        when (name) {
            "configuration.shared.cdn" -> SettingsResponse(APPLICATION_CDN, standard.render(sharedConfiguration)).asSuccess()
            "configuration.shared.json" -> SettingsResponse(APPLICATION_JSON, CdnFactory.createJsonLike().render(sharedConfiguration)).asSuccess()
            "configuration.shared.yaml" -> SettingsResponse(APPLICATION_YAML, CdnFactory.createYamlLike().render(sharedConfiguration)).asSuccess()
            else -> notFoundError("Unsupported configuration $name")
        }

    fun updateConfiguration(request: SettingsUpdateRequest): Result<Unit, ErrorResponse> =
        when (request.name) {
            "configuration.shared.cdn" -> standard.validateAndLoad(request.content, SharedConfiguration(), sharedConfiguration)
            "configuration.shared.json" -> CdnFactory.createJsonLike().validateAndLoad(request.content, SharedConfiguration(), sharedConfiguration)
            "configuration.shared.yaml" -> CdnFactory.createYamlLike().validateAndLoad(request.content, SharedConfiguration(), sharedConfiguration)
            else -> notFoundError("Unsupported configuration ${request.name}")
        }.peek {
            journalist.logger.info("Propagation | Shared configuration has updated, updating sources...")
            updateSources()
            journalist.logger.info("Propagation | Sources have been updated successfully")
        }

    private fun updateSources() {
        standard.render(sharedConfiguration)
            .takeIf { it != settingsRepository.findConfiguration(SHARED_CONFIGURATION_FILE) }
            ?.run {
                journalist.logger.info("Updating shared configuration in remote source")
                settingsRepository.saveConfiguration(SHARED_CONFIGURATION_FILE, this)
                databaseUpdateTime = Instant.now()
            }

        standard.render(sharedConfiguration)
            .takeIf { it != Files.readAllBytes(sharedFile).decodeToString() }
            ?.run {
                journalist.logger.info("Updating shared configuration in local source")
                Files.write(sharedFile, toByteArray(), WRITE, TRUNCATE_EXISTING)
            }
    }

    fun loadSharedConfiguration(): SharedConfiguration =
        sharedConfiguration.also {
            val sharedConfigurationFile = workingDirectory.resolve(SHARED_CONFIGURATION_FILE)
            val fileUpdateTime = Files.getLastModifiedTime(sharedConfigurationFile).toInstant()
            this.databaseUpdateTime = settingsRepository.findConfigurationUpdateDate(SHARED_CONFIGURATION_FILE) ?: Instant.ofEpochMilli(0)
            loadAndUpdate(fromFile = databaseUpdateTime?.isBefore(fileUpdateTime) == true)
        }

    private fun loadAndUpdate(fromFile: Boolean) {
        if (fromFile) loadFromFile() else loadFromDatabase()
        updateSources()
    }

    private fun loadFromFile() {
        journalist.logger.info("Loading shared configuration from local file")
        SettingsFileLoader.initializeAndLoad(journalist, "copy", sharedFile, workingDirectory, SHARED_CONFIGURATION_FILE, sharedConfiguration)
        journalist.logger.info("Shared configuration has been loaded from local file")
    }

    private fun loadFromDatabase() {
        journalist.logger.info("Loading shared configuration from database")
        standard.load(Source.of(settingsRepository.findConfiguration(SHARED_CONFIGURATION_FILE)), sharedConfiguration)
        journalist.logger.info("Shared configuration has been loaded from database")
    }

}