package com.reposilite.configuration.shared.infrastructure

import com.reposilite.configuration.shared.SharedConfigurationProvider
import com.reposilite.journalist.Journalist
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

const val SHARED_CONFIGURATION_FILE = "configuration.shared.json"

class LocalSharedConfigurationProvider(
    val journalist: Journalist,
    val workingDirectory: Path,
    val configurationFile: Path,
) : SharedConfigurationProvider {

    override fun updateConfiguration(content: String) {
        Files.writeString(workingDirectory.resolve(configurationFile), content)
    }

    override fun fetchConfiguration(): String =
        workingDirectory.resolve(configurationFile)
            .takeIf { it.exists() }
            ?.let { Files.readString(it) }
            ?: ""

    override fun isUpdateRequired(): Boolean =
        false

    override fun isMutable(): Boolean =
        false

    override fun name(): String =
        "local file-system"

}
