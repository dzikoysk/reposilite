package com.reposilite.storage.s3

import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageProviderFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI
import java.nio.file.Path

data class S3StorageProviderFactory(
    override val type: String,
    override val settingsType: Class<S3StorageProviderSettings>
) : StorageProviderFactory<S3StorageProvider, S3StorageProviderSettings> {
    override fun create(
        failureFacade: FailureFacade,
        workingDirectory: Path,
        repositoryName: String,
        settings: S3StorageProviderSettings
    ): S3StorageProvider {
        val client = S3Client.builder()

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

        if (settings.region.isNotEmpty()) {
            client.region(Region.of(settings.region))
        }

        if (settings.endpoint.isNotEmpty()) {
            client.endpointOverride(URI.create(settings.endpoint))
        }

        return S3StorageProvider(failureFacade, client.build(), settings.bucketName)
    }
}
