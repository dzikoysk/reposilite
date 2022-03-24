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

import com.reposilite.storage.s3.S3StorageProviderSettings
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.S3
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Integrations used in remote stack:
 * - MariaDB
 * - AWS S3 through LocalStack
 */
@Testcontainers
internal class ReposiliteRemoteIntegrationJunitExtension : Extension, BeforeEachCallback, AfterEachCallback {

    private class SpecifiedMariaDBContainer(image: String) : MariaDBContainer<SpecifiedMariaDBContainer>(DockerImageName.parse(image))

    @Container
    private val mariaDb = SpecifiedMariaDBContainer("mariadb:10.7.1")

    @Container
    private val localstack: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:0.13.2"))
        .withServices(S3)

    override fun beforeEach(context: ExtensionContext?) {
        mariaDb.start()
        localstack.start()

        context?.also {
            val instance = it.requiredTestInstance
            val type = instance::class.java

            type.getField("_extensionInitialized").set(instance, true)
            type.getField("_database").set(instance, "mysql ${mariaDb.host}:${mariaDb.getMappedPort(3306)} ${mariaDb.databaseName} ${mariaDb.username} ${mariaDb.password}")
            type.getField("_storageProvider").set(
                instance,
                S3StorageProviderSettings(
                    bucketName = "test-repository",
                    endpoint = localstack.getEndpointOverride(S3).toString(),
                    accessKey = localstack.accessKey,
                    secretKey = localstack.secretKey,
                    region = localstack.region
                )
            )
        }
    }

    override fun afterEach(context: ExtensionContext?) {
        mariaDb.stop()
        localstack.stop()
    }

}
