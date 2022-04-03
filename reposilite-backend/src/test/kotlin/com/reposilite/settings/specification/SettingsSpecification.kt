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
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.LocalConfiguration
import com.reposilite.settings.SHARED_CONFIGURATION_FILE
import com.reposilite.settings.SettingsModule
import com.reposilite.settings.SharedConfiguration
import com.reposilite.settings.createStandardSchemaGenerator
import com.reposilite.settings.infrastructure.InMemorySettingsRepository
import com.reposilite.web.http.ErrorResponse
import net.dzikoysk.cdn.KCdnFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import panda.std.Result
import java.io.File

internal abstract class SettingsSpecification {

    @TempDir
    protected lateinit var workingDirectory: File

    private val localConfiguration = LocalConfiguration()
    protected val settingsRepository = InMemorySettingsRepository()
    protected lateinit var settingsFacade: SettingsFacade
    protected val cdn = KCdnFactory.createStandard()

    @BeforeEach
    fun prepare() {
        this.settingsFacade = SettingsFacade(
            InMemoryLogger(),
            workingDirectory.toPath(),
            localConfiguration,
            lazy { throw UnsupportedOperationException() },
            settingsRepository,
            createStandardSchemaGenerator(SettingsModule())
        )

        settingsFacade.createConfigurationProvider(SharedConfiguration(), "Shared configuration", SHARED_CONFIGURATION_FILE)
    }

    fun configurationFromRepository(): String? =
        settingsRepository.findConfiguration(SHARED_CONFIGURATION_FILE)

    fun configurationFromProvider(): Result<String, ErrorResponse> =
        settingsFacade.resolveConfiguration(SHARED_CONFIGURATION_FILE).map { it.content }

    fun renderConfiguration(): String =
        cdn.render(settingsFacade.sharedConfiguration).orElseThrow { it }

}