package com.reposilite.storage

import com.reposilite.plugin.api.Facade
import com.reposilite.status.FailureFacade
import com.reposilite.storage.application.StorageProviderSettings
import java.nio.file.Path
import java.util.ServiceLoader

class StorageFacade : Facade {
    private val storageProviderFactories = ServiceLoader.load(StorageProviderFactory::class.java).associateBy { it.type }

    fun createStorageProvider(failureFacade: FailureFacade, workingDirectory: Path, repositoryName: String, storageSettings: StorageProviderSettings): StorageProvider =
        storageProviderFactories[storageSettings.type]?.create(
            failureFacade,
            workingDirectory,
            repositoryName,
            storageSettings
        ) ?: throw UnsupportedOperationException("Unknown storage provider: $storageSettings")
}

@Suppress("UNCHECKED_CAST")
private fun <T : StorageProviderSettings> StorageProviderFactory<*, T>.create(failureFacade: FailureFacade, workingDirectory: Path, repositoryName: String, storageSettings: StorageProviderSettings): StorageProvider =
    this.create(failureFacade, workingDirectory, repositoryName, storageSettings as T)
