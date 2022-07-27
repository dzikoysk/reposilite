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

package com.reposilite.configuration.specification

import com.reposilite.configuration.SharedConfigurationFacadeTest.TestSettings
import com.reposilite.configuration.application.ConfigurationComponents
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.configuration.shared.SharedSettingsProvider
import com.reposilite.configuration.shared.application.SharedConfigurationComponents
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.plugin.Extensions
import com.reposilite.status.FailureFacade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import panda.std.reactive.mutableReference
import java.io.File
import java.nio.file.Path

internal abstract class SharedConfigurationSpecification {

    @TempDir
    lateinit var workingDirectory: File

    private val logger = PrintStreamLogger(System.out, System.err)
    protected lateinit var sharedConfigurationFacade: SharedConfigurationFacade

    @BeforeEach
    fun prepare() {
        this.sharedConfigurationFacade = SharedConfigurationComponents(
            journalist = logger,
            workingDirectory = workingDirectory.toPath(),
            extensions = Extensions(logger),
            sharedConfigurationPath = Path.of("shared.json"),
            failureFacade = FailureFacade(logger),
            configurationFacade = ConfigurationComponents().configurationFacade()
        ).sharedConfigurationFacade(
            sharedSettingsProvider = SharedSettingsProvider(mapOf(TestSettings::class to mutableReference(TestSettings()))),
        )
    }

}
