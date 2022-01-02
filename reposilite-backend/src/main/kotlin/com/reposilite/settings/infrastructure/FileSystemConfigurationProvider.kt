package com.reposilite.settings.infrastructure

import com.reposilite.journalist.Journalist
import com.reposilite.settings.ConfigurationProvider
import com.reposilite.settings.api.SettingsResponse
import com.reposilite.settings.api.SettingsUpdateRequest
import com.reposilite.shared.extensions.createCdnByExtension
import com.reposilite.shared.extensions.orElseThrow
import com.reposilite.storage.getSimpleName
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.ContentType.APPLICATION_CDN
import io.javalin.http.HttpCode.BAD_REQUEST
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import net.dzikoysk.cdn.CdnException
import net.dzikoysk.cdn.source.Source
import panda.std.Result
import panda.std.Result.ok
import panda.std.Unit
import panda.std.function.ThrowingFunction
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService
import kotlin.io.path.readText


internal class FileSystemConfigurationProvider<C : Any>(
    override val name: String,
    override val displayName: String,
    private val journalist: Journalist?,
    private val workingDirectory: Path,
    private val mode: String,
    private val configurationFile: Path,
    override val configuration: C
) : ConfigurationProvider<C> {

    private val cdn = configurationFile.getSimpleName()
        .createCdnByExtension()
        .orElseThrow(ThrowingFunction.identity())

    override fun initialize() {
        journalist?.logger?.info("Loading ${displayName.lowercase()} from local file")

        load(Source.of(configurationFile))
            .peek { journalist?.logger?.info("$displayName has been loaded from local file") }
            .orElseThrow()
    }

    override fun registerWatcher(scheduler: ScheduledExecutorService) {
        // I don't think we want to support file watching
        // It can pretty much blow up the instance if file is manually modified by user
        // val watchService = FileSystems.getDefault().newWatchService() TODO?
    }

    private fun load(source: Source): Result<out C, out Exception> =
        cdn.load(source, configuration)
            .flatMap { render() }
            .map { configuration }

    fun render(): Result<String, CdnException> =
        when (mode) {
            "none" -> ok("")
            "copy" -> cdn.render(configuration, Source.of(workingDirectory.resolve(name)))
            "auto" -> cdn.render(configuration, Source.of(configurationFile))
            "print" -> cdn.render(configuration).peek { output -> printConfiguration(configurationFile, output) }
            else -> error(UnsupportedOperationException("Unknown configuration mode: $mode"))
        }

    private fun printConfiguration(file: Path, configurationSource: String) {
        if (file.readText().trim() != configurationSource.trim()) {
            println("#")
            println("# Regenerated ${displayName.lowercase()}: $file")
            println("#")
            println(configurationSource)
        }
    }

    override fun resolve(name: String): Result<SettingsResponse, ErrorResponse> =
        cdn.render(configuration)
            .map { SettingsResponse(APPLICATION_CDN, it) }
            .mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, "Cannot render ${displayName.lowercase()}: ${it.message}") }

    override fun update(request: SettingsUpdateRequest): Result<Unit, ErrorResponse> =
        when (request.name) {
            name -> load(Source.of(request.content))
                .peek { journalist?.logger?.info("Updating ${displayName.lowercase()} in local source") }
                .mapToUnit()
                .mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, "Cannot load configuration") }
            else -> errorResponse(BAD_REQUEST, "Unknown ${displayName.lowercase()}: ${request.name}")
        }

    override fun shutdown() {
        // do nothing
    }

}