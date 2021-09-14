package com.reposilite.storage.specification

import com.reposilite.ReposiliteSpecification
import com.reposilite.storage.StorageProvider

internal abstract class StorageProviderSpecification : ReposiliteSpecification() {

    protected lateinit var storageProvider: StorageProvider

}