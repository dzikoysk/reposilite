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

package com.reposilite.storage.infrastructure

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import com.reposilite.storage.StorageProviderIntegrationTest
import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.toLocation
import com.reposilite.storage.s3.S3StorageProviderSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import panda.std.ResultAssertions.assertOk
import java.io.File

@Testcontainers
internal class S3StorageProviderIntegrationTest : StorageProviderIntegrationTest() {

    @TempDir
    lateinit var rootDirectory: File

    @Container
    val floci: GenericContainer<*> = GenericContainer(DockerImageName.parse("floci/floci:latest"))
        .withExposedPorts(4566)
        .waitingFor(Wait.forListeningPort())

    @BeforeEach
    fun setup() {
        val logger = InMemoryLogger()
        val failureFacade = FailureFacade(logger)
        val storageFacade = StorageFacade()

        this.storageProvider = storageFacade.createStorageProvider(
            journalist = logger,
            failureFacade = failureFacade,
            workingDirectory = rootDirectory.toPath(),
            repository = "test-repository",
            storageSettings = S3StorageProviderSettings(
                bucketName = "test-repository",
                endpoint = "http://${floci.host}:${floci.getMappedPort(4566)}",
                accessKey = "test",
                secretKey = "test",
                region = "us-east-1"
            )
        )!!
    }

    // GH-2612: ListObjectsV2 caps a single response at 1000 keys; without continuation-token
    // pagination, directory listings and bulk deletes silently truncate beyond that.
    @Test
    fun `should list and remove all entries when directory holds more than a single S3 page`() {
        val entryCount = 1001
        val content = "x".toByteArray()

        repeat(entryCount) { i ->
            storageProvider.putFile("/snapshots/file-$i.jar".toLocation(), content.inputStream())
        }

        val listing = assertOk(storageProvider.getFileDetails("/snapshots".toLocation())) as DirectoryInfo
        assertThat(listing.files).hasSize(entryCount)

        assertOk(storageProvider.removeFile("/snapshots".toLocation()))
        assertThat(storageProvider.exists("/snapshots/file-0.jar".toLocation())).isFalse
        assertThat(storageProvider.exists("/snapshots/file-${entryCount - 1}.jar".toLocation())).isFalse
    }

}
