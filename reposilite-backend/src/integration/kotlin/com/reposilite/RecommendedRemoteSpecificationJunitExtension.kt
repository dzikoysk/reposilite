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

package com.reposilite

import com.reposilite.storage.s3.S3StorageProviderSettings
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Integrations used in remote stack:
 * - MariaDB
 * - AWS S3 through Floci
 */
@Testcontainers
internal class RecommendedRemoteSpecificationJunitExtension : Extension, BeforeEachCallback, AfterEachCallback {

    private class SpecifiedMariaDBContainer(image: String) : MariaDBContainer<SpecifiedMariaDBContainer>(DockerImageName.parse(image))

    @Container
    private val mariaDb = SpecifiedMariaDBContainer("mariadb:latest")

    @Container
    private val floci: GenericContainer<*> = GenericContainer(DockerImageName.parse("hectorvent/floci:latest"))
        .withExposedPorts(4566)

    override fun beforeEach(context: ExtensionContext?) {
        mariaDb.start()
        floci.start()

        context?.also {
            val instance = it.requiredTestInstance
            val type = instance::class.java

            type.getField("_extensionInitialized").set(instance, true)
            type.getField("_database").set(instance, "mariadb ${mariaDb.host}:${mariaDb.getMappedPort(3306)} ${mariaDb.databaseName} ${mariaDb.username} ${mariaDb.password}")
            type.getField("_storageProvider").set(
                instance,
                S3StorageProviderSettings(
                    bucketName = "test-repository",
                    endpoint = "http://${floci.host}:${floci.getMappedPort(4566)}",
                    accessKey = "test", // Floci accepts any credentials
                    secretKey = "test", // Floci accepts any credentials
                    region = "us-east-1"
                )
            )
        }
    }

    override fun afterEach(context: ExtensionContext?) {
        mariaDb.stop()
        floci.stop()
    }

}
