/*
 * Copyright (c) 2021 dzikoysk
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
package com.reposilite.config

import com.reposilite.journalist.Journalist
import net.dzikoysk.cdn.CdnFactory
import panda.utilities.StringUtils
import java.nio.file.Path

const val DEFAULT_CONFIGURATION_FILE = "reposilite.cdn"

object ConfigurationProcessor {

    fun tryLoad(journalist: Journalist, workingDirectory: Path, configurationPath: Path, mode: String): Configuration =
        try {
            val cdn = CdnFactory.createStandard()
            val configuration = cdn.load(configurationPath.toFile(), Configuration::class.java)
            verifyBasePath(configuration)

            when (mode.lowercase()) {
                "auto" -> cdn.render(configuration, configurationPath.toFile())
                "copy" -> cdn.render(configuration, workingDirectory.resolve(DEFAULT_CONFIGURATION_FILE).toFile())
                "print" -> println(cdn.render(configuration))
                "none" -> {}
                else -> journalist.logger.error("Unknown configuration mode: $mode")
            }

            configuration
        } catch (exception: Exception) {
            throw IllegalArgumentException("Cannot load configuration", exception)
        }

    private fun verifyBasePath(configuration: Configuration) {
        var basePath = configuration.basePath

        if (!StringUtils.isEmpty(basePath)) {
            if (!basePath.startsWith("/")) {
                basePath = "/$basePath"
            }

            if (!basePath.endsWith("/")) {
                basePath += "/"
            }

            configuration.basePath = basePath
        }
    }

}