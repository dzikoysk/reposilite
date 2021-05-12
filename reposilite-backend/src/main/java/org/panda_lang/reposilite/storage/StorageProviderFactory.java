package org.panda_lang.reposilite.storage;

import picocli.CommandLine;

import java.nio.file.Paths;

public final class StorageProviderFactory {

    public StorageProvider createStorageProvider(String repositoryName, String storageDescription) {
        if (storageDescription.startsWith("fs")) {
            return FileSystemStorageProvider.of(Paths.get("repositories").resolve(repositoryName), Long.MAX_VALUE); // TODO: Move quota's implementation to Repository level
        }

        if (storageDescription.startsWith("s3")) {
            S3StorageProviderSettings settings = loadConfiguration(new S3StorageProviderSettings(), storageDescription);
            return new S3StorageProvider(settings.bucketName, settings.region);
        }

        if (storageDescription.equalsIgnoreCase("rest")) {
            // TODO REST API storage endpoint
        }

        throw new UnsupportedOperationException("Unknown storage provider: " + storageDescription);
    }

    private <CONFIGURATION extends Runnable> CONFIGURATION loadConfiguration(CONFIGURATION configuration, String description) {
        CommandLine commandLine = new CommandLine(configuration);
        commandLine.execute(description.split(" "));
        return configuration;
    }

}
