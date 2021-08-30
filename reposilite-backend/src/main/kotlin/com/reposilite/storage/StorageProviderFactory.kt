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

import com.reposilite.config.ConfigurationLoader
import com.reposilite.journalist.Journalist
import com.reposilite.storage.infrastructure.FileSystemStorageProviderFactory
import com.reposilite.storage.infrastructure.S3StorageProvider
import com.reposilite.storage.infrastructure.S3StorageProviderSettings
import java.nio.file.Files
import java.nio.file.Path

internal object StorageProviderFactory {

    fun createStorageProvider(journalist: Journalist, workingDirectory: Path, storageDescription: String, quota: String): StorageProvider =
        if (storageDescription.startsWith("fs")) {
            Files.createDirectories(workingDirectory)
            FileSystemStorageProviderFactory.of(journalist, workingDirectory, quota)
        }
        else if (storageDescription.startsWith("s3")) {
            // Implement quota
            val settings = ConfigurationLoader.loadConfiguration(S3StorageProviderSettings(), storageDescription).second
            S3StorageProvider(journalist, settings.bucketName, settings.region)
        }
        // else if (storageDescription.equals("rest", ignoreCase = true)) {
        // TOFIX REST API storage endpoint
        //    null
        //}
        else throw UnsupportedOperationException("Unknown storage provider: $storageDescription")

}