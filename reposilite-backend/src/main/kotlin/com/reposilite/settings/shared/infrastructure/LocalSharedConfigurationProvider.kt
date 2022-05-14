package com.reposilite.settings.shared.infrastructure

import com.reposilite.journalist.Journalist
import com.reposilite.settings.infrastructure.FileConfigurationProvider
import panda.std.Result
import java.nio.file.Path

const val SHARED_CONFIGURATION_FILE = "configuration.shared.json"

class LocalSharedConfigurationProvider(
    journalist: Journalist,
    workingDirectory: Path,
    configurationFile: Path
) : FileConfigurationProvider(
    name = SHARED_CONFIGURATION_FILE,
    displayName = "Shared (local) configuration",
    journalist = journalist,
    workingDirectory = workingDirectory,
    configurationFile = configurationFile,
) {

    override fun initializeConfigurationFile(): Result<*, out Exception> {
        TODO("Not yet implemented")
    }

    override fun loadContent(content: String): Result<Unit, out Exception> {
        TODO("Not yet implemented")
    }

}