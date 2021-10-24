package com.reposilite.settings

import com.reposilite.journalist.Journalist
import net.dzikoysk.cdn.CdnFactory
import net.dzikoysk.cdn.shared.source.Source
import java.nio.file.Path
import kotlin.io.path.readText

internal object SettingsFileLoader {

    fun <C> initializeAndLoad(journalist: Journalist, mode: String, configurationFile: Path, workingDirectory: Path, defaultFileName: String, configuration: C): C =
        try {
            val cdn = CdnFactory.createStandard()
            val localConfiguration = cdn.load(Source.of(configurationFile), LocalConfiguration())

            when (mode) {
                "none" -> {}
                "copy" -> cdn.render(localConfiguration, workingDirectory.resolve(defaultFileName))
                "auto" -> cdn.render(localConfiguration, configurationFile)
                "print" -> {
                    val generatedConfiguration = cdn.render(localConfiguration)

                    if (configurationFile.readText().trim() != generatedConfiguration.trim()) {
                        println("#")
                        println("# Regenerated configuration: $configurationFile")
                        println("#")
                        println(generatedConfiguration)
                    }
                }
                else -> journalist.logger.error("Unknown configuration mode: $mode")
            }

            configuration
        } catch (exception: Exception) {
            throw IllegalStateException("Cannot load configuration", exception)
        }

}