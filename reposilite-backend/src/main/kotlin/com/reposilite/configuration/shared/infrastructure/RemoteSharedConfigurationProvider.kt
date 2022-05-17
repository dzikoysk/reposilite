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

package com.reposilite.configuration.shared.infrastructure

import com.reposilite.configuration.ConfigurationFacade
import com.reposilite.configuration.shared.SharedConfigurationProvider
import java.time.Instant

internal class RemoteSharedConfigurationProvider(
    private val configurationFacade: ConfigurationFacade,
) : SharedConfigurationProvider {

    companion object {
        private const val CONFIGURATION_NAME = "remote-shared-configuration"
    }

    private var databaseUpdateTime = Instant.ofEpochMilli(0)

    override fun updateConfiguration(content: String) {
        configurationFacade.saveConfiguration(CONFIGURATION_NAME, content)
    }

    override fun fetchConfiguration(): String {
        refreshUpdateTime()
        return configurationFacade.findConfiguration(CONFIGURATION_NAME) ?: ""
    }

    private fun refreshUpdateTime() {
        this.databaseUpdateTime = configurationFacade.findConfigurationUpdateDate(CONFIGURATION_NAME) ?: databaseUpdateTime
    }

    override fun isUpdateRequired(): Boolean =
        configurationFacade.findConfigurationUpdateDate(CONFIGURATION_NAME)?.isAfter(databaseUpdateTime) == true

    override fun isMutable(): Boolean =
        true

    override fun name(): String =
        "remote "

}
