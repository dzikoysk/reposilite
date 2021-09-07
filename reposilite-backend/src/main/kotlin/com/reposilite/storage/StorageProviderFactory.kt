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

import com.reposilite.config.Configuration.RepositoryConfiguration.S3StorageProviderSettings
import com.reposilite.journalist.Journalist
import com.reposilite.shared.loadCommandBasedConfiguration
import com.reposilite.storage.infrastructure.FileSystemStorageProviderFactory
import com.reposilite.storage.infrastructure.S3StorageProvider
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.nio.file.Files
import java.nio.file.Path

internal object StorageProviderFactory {

    fun createStorageProvider(journalist: Journalist, workingDirectory: Path, storageDescription: String, quota: String): StorageProvider =
        if (storageDescription.startsWith("fs")) {
            Files.createDirectories(workingDirectory)
            FileSystemStorageProviderFactory.of(journalist, workingDirectory, quota)
        }
        else if (storageDescription.startsWith("s3")) {
            // Implement quota?
            val settings = loadCommandBasedConfiguration(S3StorageProviderSettings(), storageDescription).configuration
            S3StorageProvider(journalist, createUnauthenticatedS3Client(settings.region), settings.bucketName)
        }
        // else if (storageDescription.equals("rest", ignoreCase = true)) {
        // TOFIX REST API storage endpoint
        //}
        else throw UnsupportedOperationException("Unknown storage provider: $storageDescription")

    private fun createUnauthenticatedS3Client(region: String): S3Client =
         S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .build()

}