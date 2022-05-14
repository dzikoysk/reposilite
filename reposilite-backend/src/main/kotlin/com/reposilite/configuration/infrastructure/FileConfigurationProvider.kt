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

package com.reposilite.configuration.infrastructure

import com.reposilite.journalist.Journalist
import com.reposilite.configuration.ConfigurationProvider
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.BAD_REQUEST
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import panda.std.Result
import panda.std.mapToUnit
import panda.std.orElseThrow
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService

abstract class FileConfigurationProvider(
    override val name: String,
    override val displayName: String,
    protected val journalist: Journalist?,
    protected val workingDirectory: Path,
    protected val configurationFile: Path,
) : ConfigurationProvider {

    override fun initialize(): Boolean {
        journalist?.logger?.info("Loading ${displayName.lowercase()} from local file")

        initializeConfigurationFile()
            .peek { journalist?.logger?.info("$displayName has been loaded from local file") }
            .orElseThrow()

        return true
    }

    abstract fun initializeConfigurationFile(): Result<*, out Exception>

    override fun registerWatcher(scheduler: ScheduledExecutorService) {
        // I don't think we want to support file watching
        // It can pretty much blow up the instance if file is manually modified by user
        // val watchService = FileSystems.getDefault().newWatchService()
    }

    override fun update(content: String): Result<Unit, ErrorResponse> =
        when (name) {
            name -> loadContent(content)
                .peek { journalist?.logger?.info("Updating ${displayName.lowercase()} in local source") }
                .mapToUnit()
                .mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, "Cannot load configuration") }
            else -> errorResponse(BAD_REQUEST, "Unknown ${displayName.lowercase()}: $name")
        }

    abstract fun loadContent(content: String): Result<Unit, out Exception>

}
