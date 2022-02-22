package com.reposilite.settings.api

data class S3StorageProviderSettings(
    val bucketName: String,
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val region: String
): RepositorySettings.StorageProvider {
    override val type = "s3"
}
