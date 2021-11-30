package com.reposilite.settings

import com.reposilite.shared.fs.getSimpleName
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.BAD_REQUEST
import net.dzikoysk.cdn.Cdn
import net.dzikoysk.cdn.CdnException
import net.dzikoysk.cdn.KCdnFactory
import net.dzikoysk.cdn.source.Source
import panda.std.Result
import panda.std.Result.ok
import panda.std.Unit
import java.nio.file.Path
import kotlin.io.path.readText

internal object SettingsFileLoader {

    fun <C : Any> initializeAndLoad(
        mode: String,
        configurationFile: Path,
        workingDirectory: Path,
        defaultFileName: String,
        configuration: C
    ): Result<out C, CdnException> =
        configurationFile.getSimpleName()
            .let { fileName -> when {
                fileName.endsWith(".cdn") -> KCdnFactory.createStandard()
                fileName.endsWith(".yml") || fileName.endsWith(".yaml") -> KCdnFactory.createYamlLike()
                fileName.endsWith(".json") -> KCdnFactory.createJsonLike()
                else -> throw IllegalStateException("Unknown format: $fileName")
            } }
            .let { cdn -> cdn.load(Source.of(configurationFile), configuration)
                .merge(
                    when (mode) {
                        "none" -> ok("")
                        "copy" -> cdn.render(configuration, Source.of(workingDirectory.resolve(defaultFileName)))
                        "auto" -> cdn.render(configuration, Source.of(configurationFile))
                        "print" -> cdn.render(configuration).peek { output -> printConfiguration(configurationFile, output) }
                        else -> error(UnsupportedOperationException("Unknown configuration mode: $mode"))
                    }
                ) { entity, _ -> entity }
            }


    private fun printConfiguration(file: Path, configurationSource: String) {
        if (file.readText().trim() != configurationSource.trim()) {
            println("#")
            println("# Regenerated configuration: $file")
            println("#")
            println(configurationSource)
        }
    }

    fun Cdn.validateAndLoad(source: String, testConfiguration: Any, configuration: Any): Result<Unit, ErrorResponse> =
        load(Source.of(source), testConfiguration) // validate
            .flatMap { load(Source.of(source), configuration) }
            .mapToUnit()
            .mapErr { ErrorResponse(BAD_REQUEST, "Cannot load configuration: ${it.message}") }

}