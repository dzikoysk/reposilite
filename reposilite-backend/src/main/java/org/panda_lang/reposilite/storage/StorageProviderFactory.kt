package org.panda_lang.reposilite.storage

import picocli.CommandLine
import java.nio.file.Paths

class StorageProviderFactory {

    fun createStorageProvider(repositoryName: String, storageDescription: String): StorageProvider {
        if (storageDescription.startsWith("fs")) {
            return FileSystemStorageProvider.of(
                Paths.get("repositories").resolve(repositoryName),
                Long.MAX_VALUE // TODO: Move quota's implementation to Repository level
            )
        }

        if (storageDescription.startsWith("s3")) {
            val settings = loadConfiguration(S3StorageProviderSettings(), storageDescription)
            return S3StorageProvider(settings.bucketName, settings.region)
        }

        if (storageDescription.equals("rest", ignoreCase = true)) {
            // TODO REST API storage endpoint
        }

        throw UnsupportedOperationException("Unknown storage provider: $storageDescription")
    }

    private fun <CONFIGURATION : Runnable> loadConfiguration(configuration: CONFIGURATION, description: String): CONFIGURATION =
        CommandLine.populateCommand(configuration, *description.split(" ").toTypedArray())

}