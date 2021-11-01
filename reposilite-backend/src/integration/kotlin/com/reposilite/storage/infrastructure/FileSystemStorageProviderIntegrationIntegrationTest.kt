/*
 * Copyright (c) 2021 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.storage.infrastructure

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.storage.StorageProviderFactory
import com.reposilite.storage.StorageProviderIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class FileSystemStorageProviderIntegrationIntegrationTest : StorageProviderIntegrationTest() {

    @TempDir
    lateinit var rootDirectory: File

    @BeforeEach
    fun setup() {
        super.storageProvider = StorageProviderFactory.createStorageProvider(InMemoryLogger(), rootDirectory.toPath(), "test-storage", "--quota 1MB")
    }

}