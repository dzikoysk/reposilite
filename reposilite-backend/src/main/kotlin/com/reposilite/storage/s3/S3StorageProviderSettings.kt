package com.reposilite.storage.s3

import com.reposilite.settings.shared.Doc
import com.reposilite.storage.StorageProviderSettings

@Doc(title = "S3 Storage Provider", description = "Amazon S3 storage provider settings")
data class S3StorageProviderSettings(
    override val type: String = "s3",
    @Doc(title = "Bucket", description = "The selected AWS bucket")
    val bucketName: String = "",
    @Doc(title = "Endpoint", description = "Overwrite the AWS endpoint (optional)")
    val endpoint: String = "",
    @Doc(title = "Access Key", description = "Overwrite AWS access-key used to authenticate (optional)")
    val accessKey: String = "",
    @Doc(title = "Secret Key", description = "Overwrite AWS secret-key used to authenticate (optional)")
    val secretKey: String = "",
    @Doc(title = "Region", description = "Overwrite AWS region (optional)")
    val region: String = ""
): StorageProviderSettings