/*
 * Copyright (c) 2022 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.settings.infrastructure

import com.reposilite.journalist.Journalist
import com.reposilite.settings.ConfigurationProvider
import com.reposilite.shared.extensions.createCdnByExtension
import com.reposilite.storage.getSimpleName
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.BAD_REQUEST
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import net.dzikoysk.cdn.CdnException
import net.dzikoysk.cdn.source.Source
import panda.std.Result
import panda.std.Result.ok
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

        /*load(Source.of(configurationFile))
            .peek { journalist?.logger?.info("$displayName has been loaded from local file") }
            .orElseThrow()*/// TODO reimplement loading without cdn
    }

    override fun registerWatcher(scheduler: ScheduledExecutorService) {
        // I don't think we want to support file watching
        // It can pretty much blow up the instance if file is manually modified by user
        // val watchService = FileSystems.getDefault().newWatchService()
    }

    private fun load(source: Source): Result<out C, out Exception> =
        cdn.load(source, configuration)
            .flatMap { render() }
            .map { configuration }

    private fun render(): Result<String, CdnException> =
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

    override fun resolve(name: String): Result<String, ErrorResponse> =
        cdn.render(configuration)
            .mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, "Cannot render ${displayName.lowercase()}: ${it.message}") }

    @Suppress("UNUSED_EXPRESSION")
    override fun update(name: String, content: String): Result<Unit, ErrorResponse> =
        when (name) {
            /*name -> load(Source.of(content))
                .peek { journalist?.logger?.info("Updating ${displayName.lowercase()} in local source") }
                .mapToUnit()
                .mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, "Cannot load configuration") }*/// fixme
            else -> errorResponse(BAD_REQUEST, "Unknown ${displayName.lowercase()}: $name")
        }

    override fun shutdown() {
        // do nothing
    }

}
