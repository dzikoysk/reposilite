package com.reposilite.storage.infrastructure

import com.reposilite.ReposiliteLocalIntegrationJunitExtension
import com.reposilite.storage.StorageProviderTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class FileSystemStorageProviderIntegrationTest : StorageProviderTest() {

    @TempDir
    lateinit var rootDirectory: File

    @BeforeEach
    fun setup() {
        super.storageProvider = FileSystemStorageProviderFactory.of(rootDirectory.toPath(), 1024L * 1024L)
    }

}