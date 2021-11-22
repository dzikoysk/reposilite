package com.reposilite.settings

import com.reposilite.journalist.Journalist
import com.reposilite.shared.fs.getSimpleName
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.BAD_REQUEST
import net.dzikoysk.cdn.Cdn
import net.dzikoysk.cdn.KCdnFactory
import net.dzikoysk.cdn.source.Source
import panda.std.Result
import java.nio.file.Path
import kotlin.io.path.readText

internal object SettingsFileLoader {

    fun <C : Any> initializeAndLoad(journalist: Journalist, mode: String, configurationFile: Path, workingDirectory: Path, defaultFileName: String, configuration: C): C =
        try {
            val fileName = configurationFile.getSimpleName()

            val cdn = when {
                fileName.endsWith(".cdn") -> KCdnFactory.createStandard()
                fileName.endsWith(".yml") || fileName.endsWith(".yaml") -> KCdnFactory.createYamlLike()
                fileName.endsWith(".json") -> KCdnFactory.createJsonLike()
                else -> throw IllegalStateException("Unknown format: $fileName")

            }

            cdn.load(Source.of(configurationFile), configuration)

            when (mode) {
                "none" -> {}
                "copy" -> cdn.render(configuration, workingDirectory.resolve(defaultFileName))
                "auto" -> cdn.render(configuration, configurationFile)
                "print" -> {
                    val generatedConfiguration = cdn.render(configuration)

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

    fun Cdn.validateAndLoad(source: String, testConfiguration: Any, configuration: Any): Result<Unit, ErrorResponse> =
        try {
            load(Source.of(source), testConfiguration) // validate
            load(Source.of(source), configuration)
            Result.ok(Unit)
        } catch (exception: Exception) {
            errorResponse(BAD_REQUEST, "Cannot load configuration: ${exception.message}")
        }

}