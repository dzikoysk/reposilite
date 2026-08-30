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

package com.reposilite.maven

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.journalist.Journalist
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.api.REPOSITORY_NAME_MAX_LENGTH
import com.reposilite.plugin.Extensions
import com.reposilite.repository.RepositoryFacade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.shared.notFoundError
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import com.reposilite.storage.s3.S3StorageProviderSettings
import com.reposilite.storage.s3.findS3SharedBucketConflicts
import java.nio.file.Path
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference

internal class RepositoryProvider(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    private val remoteClientProvider: RemoteClientProvider,
    private val authenticationFacade: AuthenticationFacade,
    extensions: Extensions,
    private val failureFacade: FailureFacade,
    statisticsFacade: StatisticsFacade,
    private val storageFacade: StorageFacade,
    mirrorService: MirrorService,
    resolutionProvider: ResolutionProvider,
    private val repositoryFacade: RepositoryFacade,
    repositoriesSource: Reference<List<RepositorySettings>>,
) {

    val repositoryService = RepositoryService(
        journalist = journalist,
        repositoryProvider = this,
        repositoryFacade = repositoryFacade,
        mirrorService = mirrorService,
        resolutionProvider = resolutionProvider,
        statisticsFacade = statisticsFacade,
        extensions = extensions
    )

    @Volatile
    private var repositories: Map<String, Repository> = createRepositories(repositoriesSource.get())

    init {
        repositoriesSource.subscribe { settings ->
            val updatedRepositories = createRepositories(settings)
            val previousRepositories = repositories
            repositories = updatedRepositories
            previousRepositories.values.forEach { it.shutdown() }
        }
    }

    private fun createRepositories(repositoriesConfiguration: List<RepositorySettings>): Map<String, Repository> {
        val factory = RepositoryFactory(
            journalist = journalist,
            workingDirectory = workingDirectory,
            authenticationFacade = authenticationFacade,
            remoteClientProvider = remoteClientProvider,
            failureFacade = failureFacade,
            storageFacade = storageFacade,
            repositoryService = repositoryService,
            repositoriesNames = repositoriesConfiguration.map { it.id },
        )

        val duplicatedNames = repositoriesConfiguration.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        val sharedBucketConflicts = findS3SharedBucketConflicts(
            repositoriesConfiguration.mapNotNull { configuration ->
                (configuration.storageProvider as? S3StorageProviderSettings)?.let { configuration.id to it }
            }
        )

        return repositoriesConfiguration.asSequence()
            .mapNotNull { configuration ->
                runCatching {
                    require(configuration.id !in duplicatedNames) {
                        "Repository name '${configuration.id}' is duplicated in Maven repository settings"
                    }
                    require(configuration.id !in sharedBucketConflicts) {
                        "Its S3 key namespace overlaps another repository sharing the same bucket. Give each repository a distinct 'prefix', or enable 'sharedBucket' on every repository sharing the bucket"
                    }
                    require(configuration.id.length < REPOSITORY_NAME_MAX_LENGTH) {
                        "Repository name cannot exceed $REPOSITORY_NAME_MAX_LENGTH characters"
                    }
                    repositoryFacade.validateRepositoryName("maven", configuration.id)

                    factory.createRepository(configuration.id, configuration)
                }
                    .onFailure { failureFacade.throwException("Cannot load ${configuration.id} repository", it) }
                    .getOrNull()
            }
            .associateBy { it.name }
    }


    fun findRepository(name: String): Result<Repository, ErrorResponse> =
        getRepository(name)
            ?.asSuccess()
            ?: notFoundError("Repository $name not found")

    fun getRepository(name: String): Repository? =
        repositories[name]

    fun getRepositories(): Collection<Repository> =
        repositories.values

}
