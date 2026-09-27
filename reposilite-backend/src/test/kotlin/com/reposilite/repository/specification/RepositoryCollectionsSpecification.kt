/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.repository.specification

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.repository.RepositoryCollections
import com.reposilite.repository.api.RepositoryConfiguration
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageProviderSettings
import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import panda.std.reactive.Reference
import java.util.function.Supplier

internal abstract class RepositoryCollectionsSpecification {

    protected val failures = FailureFacade(InMemoryLogger())
    protected val collections = RepositoryCollections(failures)
    protected val operations = mutableListOf<String>()

    protected fun useRepositories(settings: Reference<List<TestSettings>>): Supplier<Map<String, TestRepository>> =
        collections.register(
            source = settings,
            create = { configurations ->
                configurations.associate { configuration ->
                    operations += "start:${configuration.id}"
                    configuration.id to TestRepository()
                }
            },
            shutdown = { repository ->
                operations += "stop"
                check(!repository.failShutdown) { "Cannot close repository" }
                repository.closed = true
            },
        )

    protected data class TestSettings(
        override val id: String,
        override val storageProvider: StorageProviderSettings = FileSystemStorageProviderSettings(),
    ) : RepositoryConfiguration

    protected class TestRepository {
        var closed = false
        var failShutdown = false
    }

}
