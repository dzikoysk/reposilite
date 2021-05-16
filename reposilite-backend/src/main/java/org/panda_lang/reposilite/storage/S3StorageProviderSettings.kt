package org.panda_lang.reposilite.storage

import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "s3", description = ["Amazon S3 storage provider settings"])
internal class S3StorageProviderSettings : Runnable {

    @Parameters(index = "0", paramLabel = "<bucket-name>")
    lateinit var bucketName: String

    @Parameters(index = "1", paramLabel = "<region>")
    lateinit var region: String

    override fun run() { /* Validation */ }

}