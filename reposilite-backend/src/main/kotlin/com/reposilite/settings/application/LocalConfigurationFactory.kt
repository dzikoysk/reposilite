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

package com.reposilite.settings.application

import com.reposilite.ReposiliteParameters
import com.reposilite.settings.api.LOCAL_CONFIGURATION_FILE
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.infrastructure.FileSystemConfigurationProvider

internal object LocalConfigurationFactory {

    fun createLocalConfiguration(parameters: ReposiliteParameters): LocalConfiguration =
        FileSystemConfigurationProvider(
            LOCAL_CONFIGURATION_FILE,
            "Local configuration",
            null,
            parameters.workingDirectory,
            parameters.localConfigurationMode,
            parameters.localConfigurationPath,
            LocalConfiguration()
        )
        .also { it.initialize() }
        .configuration

}