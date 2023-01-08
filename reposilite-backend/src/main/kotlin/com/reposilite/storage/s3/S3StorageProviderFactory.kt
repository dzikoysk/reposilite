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

package com.reposilite.storage.s3

import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageProviderFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI
import java.nio.file.Path

class S3StorageProviderFactory : StorageProviderFactory<S3StorageProvider, S3StorageProviderSettings> {

    override fun create(failureFacade: FailureFacade, workingDirectory: Path, repositoryName: String, settings: S3StorageProviderSettings): S3StorageProvider {
        val client = S3Client.builder()

        if (System.getProperty("reposilite.s3.pathStyleAccessEnabled") == "true") {
            client.serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build()
            )
        }

        if (settings.accessKey.isNotEmpty() && settings.secretKey.isNotEmpty()) {
            client.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        settings.accessKey,
                        settings.secretKey
                    )
                )
            )
        }

        when {
            settings.region.isNotEmpty() -> client.region(Region.of(settings.region))
            else -> client.region(Region.of("reposilite"))
        }

        if (settings.endpoint.isNotEmpty()) {
            client.endpointOverride(URI.create(settings.endpoint))
        }

        return S3StorageProvider(failureFacade, client.build(), settings.bucketName)
    }

    override val settingsType: Class<S3StorageProviderSettings> =
        S3StorageProviderSettings::class.java

    override val type: String =
        "s3"

}
