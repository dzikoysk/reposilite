package com.reposilite.configuration.shared.infrastructure

import com.reposilite.configuration.infrastructure.FileConfigurationProvider
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.journalist.Journalist
import panda.std.Result
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

const val SHARED_CONFIGURATION_FILE = "configuration.shared.json"

class LocalSharedConfigurationProvider(
    journalist: Journalist,
    workingDirectory: Path,
    configurationFile: Path,
    val sharedConfigurationFacade: SharedConfigurationFacade
) : FileConfigurationProvider(
    name = SHARED_CONFIGURATION_FILE,
    displayName = "Shared (local) configuration",
    journalist = journalist,
    workingDirectory = workingDirectory,
    configurationFile = configurationFile,
) {

    override fun initializeConfigurationFile(): Result<*, out Exception> =
        workingDirectory.resolve(configurationFile)
            .let { Result.`when`<Path, Exception>(it.exists(), { it }, { IllegalStateException("$configurationFile does not exist") }) }
            .map { Files.readString(it) }
            .flatMap { sharedConfigurationFacade.updateSharedSettings(it) }

    override fun loadContent(content: String): Result<Unit, out Exception> =
        sharedConfigurationFacade.updateSharedSettings(content)

}