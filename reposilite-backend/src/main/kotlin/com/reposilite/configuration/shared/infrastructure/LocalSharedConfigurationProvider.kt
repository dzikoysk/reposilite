/*
 * Copyright (c) 2023 dzikoysk
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
