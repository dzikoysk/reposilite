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

import com.reposilite.configuration.infrastructure.FileConfigurationProvider
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.local.LocalConfigurationMode
import com.reposilite.configuration.local.LocalConfigurationMode.AUTO
import com.reposilite.configuration.local.LocalConfigurationMode.NONE
import com.reposilite.journalist.Journalist
import com.reposilite.shared.extensions.createCdnByExtension
import com.reposilite.storage.getSimpleName
import net.dzikoysk.cdn.CdnException
import net.dzikoysk.cdn.source.Source
import panda.std.Result
import panda.std.Result.ok
import panda.std.function.ThrowingFunction
import panda.std.mapToUnit
import java.nio.file.Path

const val LOCAL_CONFIGURATION_FILE = "configuration.cdn"

internal class LocalConfigurationProvider(
    journalist: Journalist?,
    workingDirectory: Path,
    configurationFile: Path,
    private val mode: LocalConfigurationMode,
    val localConfiguration: LocalConfiguration
) : FileConfigurationProvider(
    name = LOCAL_CONFIGURATION_FILE,
    displayName = "Local configuration",
    journalist = journalist,
    workingDirectory = workingDirectory,
    configurationFile = configurationFile,
) {

    private val cdn = configurationFile.getSimpleName()
        .createCdnByExtension()
        .orElseThrow(ThrowingFunction.identity())

    override fun initializeConfigurationFile(): Result<*, out Exception> =
        load(Source.of(configurationFile))

    private fun load(source: Source): Result<LocalConfiguration, out Exception> =
        cdn.load(source, localConfiguration)
            .flatMap { render() }
            .map { localConfiguration }

    private fun render(): Result<String, CdnException> =
        when (mode) {
            NONE -> ok("")
            AUTO -> cdn.render(localConfiguration, Source.of(configurationFile))
        }

    override fun loadContent(content: String): Result<Unit, out Exception> =
        load(Source.of(content)).mapToUnit()

}
