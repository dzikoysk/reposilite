package com.reposilite.settings.api

data class FSStorageProviderSettings(
    val quota: String,
    val mount: String
): RepositorySettings.StorageProvider {
    override val type = "fs"
}
