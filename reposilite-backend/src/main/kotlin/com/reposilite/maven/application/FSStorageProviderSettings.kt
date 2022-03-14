package com.reposilite.maven.application

data class FSStorageProviderSettings(
    val quota: String = "100%",
    val mount: String = ""
): RepositorySettings.StorageProvider {
    override val type = "fs"
}
