package com.reposilite.maven.application

import com.reposilite.settings.api.Doc

@Doc(title = "S3 Storage Provider", description = "Amazon S3 storage provider settings")
data class S3StorageProviderSettings(
    val bucketName: String,
    @Doc(title = "Endpoint", description = "overwrite the AWS endpoint (optional)")
    val endpoint: String = "",
    @Doc(title = "Access Key", description = "overwrite AWS access-key used to authenticate (optional)")
    val accessKey: String = "",
    @Doc(title = "Secret Key", description = "overwrite AWS secret-key used to authenticate (optional)")
    val secretKey: String = "",
    @Doc(title = "Region", description = "overwrite AWS region (optional)")
    val region: String = ""
): RepositorySettings.StorageProvider {
    override val type = "s3"
}
