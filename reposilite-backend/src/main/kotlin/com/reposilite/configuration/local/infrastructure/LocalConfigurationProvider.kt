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

package com.reposilite.configuration.local.infrastructure

import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.local.LocalConfigurationMode
import com.reposilite.configuration.local.LocalConfigurationMode.AUTO
import com.reposilite.configuration.local.LocalConfigurationMode.NONE
import com.reposilite.journalist.Journalist
import net.dzikoysk.cdn.CdnException
import net.dzikoysk.cdn.KCdnFactory
import net.dzikoysk.cdn.source.Source
import panda.std.Result
import panda.std.Result.ok
import panda.std.orThrow
import java.nio.file.Path

const val LOCAL_CONFIGURATION_FILE = "configuration.cdn"

internal class LocalConfigurationProvider(
    val journalist: Journalist?,
    val workingDirectory: Path,
    val configurationFile: Path,
    private val mode: LocalConfigurationMode,
    val localConfiguration: LocalConfiguration
) {

    private val cdn = KCdnFactory.createStandard()

    fun initialize() {
        workingDirectory.resolve(configurationFile)
            .let { Source.of(it) }
            .also { journalist?.logger?.info("Loading local configuration from local file") }
            .let { cdn.load(it, localConfiguration) }
            .peek { render() }
            .peek { journalist?.logger?.info("Local configuration has been loaded from local file") }
            .orThrow()
    }

    private fun render(): Result<String, CdnException> =
        when (mode) {
            NONE -> ok("")
            AUTO -> cdn.render(localConfiguration, Source.of(configurationFile))
        }

}
