package com.reposilite.storage

import com.reposilite.plugin.api.Facade
import com.reposilite.status.FailureFacade
import java.nio.file.Path
import java.util.ServiceLoader

class StorageFacade : Facade {

    @Suppress("UNCHECKED_CAST")
    private val storageProviderFactories = ServiceLoader.load(StorageProviderFactory::class.java)
        .associateBy { it.type }
        .mapValues { (_, factory) -> factory as StorageProviderFactory<*, StorageProviderSettings> }

    fun createStorageProvider(failureFacade: FailureFacade, workingDirectory: Path, repository: String, storageSettings: StorageProviderSettings): StorageProvider? =
        storageProviderFactories[storageSettings.type]?.create(failureFacade, workingDirectory, repository, storageSettings)

}