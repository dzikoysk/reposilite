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

package com.reposilite.configuration.local

import com.reposilite.ReposiliteParameters
import com.reposilite.configuration.local.infrastructure.LocalConfigurationProvider
import com.reposilite.journalist.Journalist

internal object LocalConfigurationFactory {

    fun createLocalConfiguration(journalist: Journalist?, parameters: ReposiliteParameters): LocalConfiguration =
        LocalConfigurationProvider(
            journalist = journalist,
            workingDirectory = parameters.workingDirectory,
            configurationFile = parameters.localConfigurationPath,
            mode = parameters.localConfigurationMode,
            localConfiguration = LocalConfiguration()
        )
        .also { it.initialize() }
        .localConfiguration

}