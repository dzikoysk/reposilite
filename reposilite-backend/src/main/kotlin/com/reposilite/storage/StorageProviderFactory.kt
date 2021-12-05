/*
 * Copyright (c) 2021 dzikoysk
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

package com.reposilite.storage

import com.reposilite.journalist.Journalist
import com.reposilite.settings.api.SharedConfiguration.RepositoryConfiguration.FSStorageProviderSettings
import com.reposilite.settings.api.SharedConfiguration.RepositoryConfiguration.S3StorageProviderSettings
import com.reposilite.shared.extensions.loadCommandBasedConfiguration
import com.reposilite.storage.infrastructure.FileSystemStorageProvider
import com.reposilite.storage.infrastructure.FileSystemStorageProviderFactory
import com.reposilite.storage.infrastructure.S3StorageProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

object StorageProviderFactory {

    fun createStorageProvider(journalist: Journalist, workingDirectory: Path, repositoryName: String, storageDescription: String): StorageProvider =
        when {
            storageDescription.startsWith("fs") -> createFileSystemStorageProvider(workingDirectory, repositoryName, storageDescription)
            storageDescription.startsWith("s3") -> createS3StorageProvider(journalist, storageDescription)
            else -> throw UnsupportedOperationException("Unknown storage provider: $storageDescription")
        }

    private fun createFileSystemStorageProvider(workingDirectory: Path, repositoryName: String, storageDescription: String): FileSystemStorageProvider {
        val settings = loadCommandBasedConfiguration(FSStorageProviderSettings(), storageDescription).configuration

        val repositoryDirectory =
            if (settings.mount.isEmpty())
                workingDirectory.resolve(repositoryName)
            else
                workingDirectory.resolve(settings.mount)

        Files.createDirectories(repositoryDirectory)
        return FileSystemStorageProviderFactory.of(repositoryDirectory, settings.quota)
    }

    private fun createS3StorageProvider(journalist: Journalist, storageDescription: String): S3StorageProvider {
        val settings = loadCommandBasedConfiguration(S3StorageProviderSettings(), storageDescription).configuration
        val client = S3Client.builder()

        if (settings.accessKey.isNotEmpty() && settings.secretKey.isNotEmpty()) {
            client.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(settings.accessKey, settings.secretKey)))
        }

        if (settings.region.isNotEmpty()) {
            client.region(Region.of(settings.region))
        }

        if (settings.endpoint.isNotEmpty()) {
            client.endpointOverride(URI.create(settings.endpoint))
        }

        return S3StorageProvider(journalist, client.build(), settings.bucketName)
    }

}