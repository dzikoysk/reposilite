package org.panda_lang.reposilite.storage;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "s3", description = "Amazon S3 storage provider settings")
final class S3StorageProviderSettings implements Runnable {

    @Parameters(index = "0", paramLabel = "<bucket-name>")
    String bucketName;

    @Parameters(index = "1", paramLabel = "<region>")
    String region;

    @Override
    public void run() {
        // validation
    }

}
