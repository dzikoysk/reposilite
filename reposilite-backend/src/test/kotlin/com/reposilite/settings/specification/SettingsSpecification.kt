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

package com.reposilite.settings.specification

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.settings.ConfigurationService
import com.reposilite.settings.SHARED_CONFIGURATION_FILE
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.SettingsService
import com.reposilite.settings.SharedConfiguration
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.createSharedConfigurationSchemaGenerator
import com.reposilite.settings.infrastructure.InMemoryConfigurationRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.Executors

internal abstract class SettingsSpecification {

    @TempDir
    protected lateinit var workingDirectory: File

    private val localConfiguration = LocalConfiguration()
    private lateinit var settingsFacade: SettingsFacade

    @BeforeEach
    fun prepare() {
        val logger = InMemoryLogger()

        this.settingsFacade = SettingsFacade(
            journalist = InMemoryLogger(),
            configurationService = ConfigurationService(
                logger,
                workingDirectory.toPath(),
                InMemoryConfigurationRepository(),
                Executors.newScheduledThreadPool(1),
                localConfiguration
            ),
            settingsService = SettingsService(createSharedConfigurationSchemaGenerator())
        )

        settingsFacade.createConfigurationProvider(SharedConfiguration(), "Shared configuration", SHARED_CONFIGURATION_FILE)
    }

}