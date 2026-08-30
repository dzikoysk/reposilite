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

package com.reposilite.generic

import com.reposilite.generic.application.GenericRepositorySettings
import com.reposilite.journalist.Journalist
import com.reposilite.repository.RepositoryFacade
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import com.reposilite.storage.s3.S3StorageProviderSettings
import com.reposilite.storage.s3.findS3SharedBucketConflicts
import panda.std.reactive.Reference
import java.nio.file.Path

internal class GenericRepositoryStore(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    private val failureFacade: FailureFacade,
    private val repositoryFacade: RepositoryFacade,
    private val storageFacade: StorageFacade,
    repositoriesSource: Reference<List<GenericRepositorySettings>>,
) {

    @Volatile
    private var repositories = createRepositories(repositoriesSource.get())

    init {
        repositoriesSource.subscribe { settings ->
            val updatedRepositories = createRepositories(settings)
            val previousRepositories = repositories
            repositories = updatedRepositories
            previousRepositories.values.forEach { it.storageProvider.shutdown() }
        }
    }

    fun findRepository(name: String): GenericRepository? =
        repositories[name]

    fun getRepositories(): Collection<GenericRepository> =
        repositories.values

    fun shutdown() =
        repositories.values.forEach { it.storageProvider.shutdown() }

    private fun createRepositories(settings: List<GenericRepositorySettings>): Map<String, GenericRepository> {
        val duplicatedNames = settings.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        val sharedBucketConflicts = findS3SharedBucketConflicts(
            settings.mapNotNull { configuration ->
                (configuration.storageProvider as? S3StorageProviderSettings)?.let { configuration.id to it }
            }
        )

        return settings.mapNotNull { configuration ->
            runCatching {
                require(configuration.id !in duplicatedNames) {
                    "Repository name '${configuration.id}' is duplicated in generic repository settings"
                }
                require(configuration.id !in sharedBucketConflicts) {
                    "Its S3 key namespace overlaps another generic repository sharing the same bucket"
                }
                repositoryFacade.validateRepositoryName("generic", configuration.id)

                GenericRepository(
                    name = configuration.id,
                    visibility = configuration.visibility,
                    redeployment = configuration.redeployment,
                    storageProvider = storageFacade.createStorageProvider(
                        journalist = journalist,
                        failureFacade = failureFacade,
                        workingDirectory = workingDirectory.resolve("repositories"),
                        repository = configuration.id,
                        storageSettings = configuration.storageProvider,
                    ) ?: throw IllegalArgumentException("Unknown storage provider '${configuration.storageProvider.type}'"),
                )
            }
                .onFailure { failureFacade.throwException("Cannot load ${configuration.id} repository", it) }
                .getOrNull()
        }.associateBy { it.name }
    }
}
