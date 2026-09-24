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
import com.reposilite.repository.api.RepositoryIdentity
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import panda.std.reactive.Reference
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

internal class GenericRepositories(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    private val failureFacade: FailureFacade,
    private val storageFacade: StorageFacade,
    repositoriesSource: Reference<List<GenericRepositorySettings>>,
) {

    private val repositories = AtomicReference(createRepositories(repositoriesSource.get()))

    init {
        repositoriesSource.subscribe { settings ->
            shutdown()
            repositories.set(createRepositories(settings))
        }
    }

    fun findRepository(name: String): GenericRepository? =
        repositories.get()[name]

    fun getRepositories(): Collection<GenericRepository> =
        repositories.get().values

    fun shutdown() =
        repositories.get().values.forEach { it.storageProvider.shutdown() }

    private fun createRepositories(settings: List<GenericRepositorySettings>): Map<String, GenericRepository> {
        val duplicatedNames = settings.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys

        return settings.mapNotNull { configuration ->
            val identity = RepositoryIdentity.create(configuration.id)
                .onError { failureFacade.throwException("Cannot load ${configuration.id} repository", IllegalArgumentException(it)) }
                .orNull() ?: return@mapNotNull null

            runCatching {
                require(configuration.id !in duplicatedNames) {
                    "Repository name '${configuration.id}' is duplicated in generic repository settings"
                }
                GenericRepository(
                    identity = identity,
                    visibility = configuration.visibility,
                    redeployment = configuration.redeployment,
                    storageProvider = storageFacade.createStorageProvider(
                        journalist = journalist,
                        failureFacade = failureFacade,
                        workingDirectory = workingDirectory.resolve("repositories"),
                        repository = identity.name,
                        storageSettings = configuration.storageProvider,
                    ) ?: throw IllegalArgumentException("Unknown storage provider '${configuration.storageProvider.type}'"),
                )
            }
                .onFailure { failureFacade.throwException("Cannot load ${configuration.id} repository", it) }
                .getOrNull()
        }.associateBy { it.name }
    }
}
