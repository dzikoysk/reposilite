/*
 * Copyright (c) 2020-2026 dzikoysk
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

import com.reposilite.configuration.local.infrastructure.LOCAL_CONFIGURATION_FILE
import com.reposilite.configuration.shared.infrastructure.SHARED_CONFIGURATION_FILE
import com.reposilite.createWithParameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class ParametersTest {

    @TempDir
    lateinit var workingDirectory: File

    @Test
    fun `should create default local configuration`() {
        createWithParameters("--working-directory=$workingDirectory", "--generate-configuration=local")
        val localConfiguration = workingDirectory.resolve(LOCAL_CONFIGURATION_FILE)
        assertThat(localConfiguration.exists()).isTrue
        assertThat(localConfiguration.readText()).contains("Reposilite :: Local")
    }

    @Test
    fun `should create default shared configuration`() {
        createWithParameters("--working-directory=$workingDirectory", "--generate-configuration=shared")
        val sharedConfiguration = workingDirectory.resolve(SHARED_CONFIGURATION_FILE)
        assertThat(sharedConfiguration.exists()).isTrue
        assertThat(sharedConfiguration.readText()).contains("\"web\"")
    }

}
