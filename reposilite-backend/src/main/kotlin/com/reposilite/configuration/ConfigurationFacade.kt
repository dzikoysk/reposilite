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

package com.reposilite.configuration

import com.reposilite.plugin.api.Facade
import java.time.Instant

class ConfigurationFacade internal constructor(
    private val configurationRepository: ConfigurationRepository
) : Facade {

    fun saveConfiguration(name: String, configuration: String) =
        configurationRepository.saveConfiguration(name, configuration)

    fun findConfiguration(name: String): String? =
        configurationRepository.findConfiguration(name)

    fun findConfigurationUpdateDate(name: String): Instant? =
        configurationRepository.findConfigurationUpdateDate(name)

}
