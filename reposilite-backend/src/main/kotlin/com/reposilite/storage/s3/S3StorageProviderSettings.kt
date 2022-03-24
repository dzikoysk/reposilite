package com.reposilite.storage.s3

import com.reposilite.settings.api.Doc
import com.reposilite.storage.application.StorageProviderSettings
import net.dzikoysk.cdn.entity.Contextual

@Contextual
@Doc(title = "S3 Storage Provider", description = "Amazon S3 storage provider settings")
data class S3StorageProviderSettings(
    override val type: String = "s3",
    @Doc(title = "Bucket", description = "the selected AWS bucket")
    val bucketName: String = "",
    @Doc(title = "Endpoint", description = "overwrite the AWS endpoint (optional)")
    val endpoint: String = "",
    @Doc(title = "Access Key", description = "overwrite AWS access-key used to authenticate (optional)")
    val accessKey: String = "",
    @Doc(title = "Secret Key", description = "overwrite AWS secret-key used to authenticate (optional)")
    val secretKey: String = "",
    @Doc(title = "Region", description = "overwrite AWS region (optional)")
    val region: String = ""
): StorageProviderSettings