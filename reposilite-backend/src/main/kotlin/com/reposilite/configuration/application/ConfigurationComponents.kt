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

package com.reposilite.configuration.application

import com.reposilite.configuration.ConfigurationFacade
import com.reposilite.configuration.ConfigurationRepository
import com.reposilite.configuration.infrastructure.InMemoryConfigurationRepository
import com.reposilite.configuration.infrastructure.SqlConfigurationRepository
import org.jetbrains.exposed.sql.Database

class ConfigurationComponents(private val database: Database? = null) {

    private fun configurationRepository(): ConfigurationRepository =
        when (database) {
            null -> InMemoryConfigurationRepository()
            else -> SqlConfigurationRepository(database)
        }

    fun configurationFacade(configurationRepository: ConfigurationRepository = configurationRepository()): ConfigurationFacade =
        ConfigurationFacade(configurationRepository)

}
