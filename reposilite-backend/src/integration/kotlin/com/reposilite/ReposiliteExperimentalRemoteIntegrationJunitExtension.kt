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

package com.reposilite

import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Integrations used in remote stack:
 * - PostreSQL
 * - Local file system
 */
@Testcontainers
internal class ReposiliteExperimentalRemoteIntegrationJunitExtension : Extension, BeforeEachCallback, AfterEachCallback {

    private class SpecifiedPostgreSQLContainer(image: String) : PostgreSQLContainer<SpecifiedPostgreSQLContainer>(DockerImageName.parse(image))

    @Container
    private val postgres = SpecifiedPostgreSQLContainer("postgres:13.6")

    override fun beforeEach(context: ExtensionContext?) {
        postgres.start()

        context?.also {
            val instance = it.requiredTestInstance
            val type = instance::class.java

            type.getField("_extensionInitialized").set(instance, true)
            type.getField("_database").set(instance, "postgresql ${postgres.host}:${postgres.getMappedPort(5432)} ${postgres.databaseName} ${postgres.username} ${postgres.password}")
            type.getField("_storageProvider").set(instance, FileSystemStorageProviderSettings())
        }
    }

    override fun afterEach(context: ExtensionContext) {
        postgres.stop()
    }

}