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
import com.reposilite.plugin.Extensions
import com.reposilite.repository.RepositoryFacade
import com.reposilite.repository.api.RepositoryIdentity
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import panda.std.Result.supplyThrowing
import panda.std.reactive.Reference

internal class MavenRepositories(
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
    repositoryFacade: RepositoryFacade,
    repositoriesSource: Reference<List<RepositorySettings>>,
) {

    val repositoryService = RepositoryService(
        journalist = journalist,
        repositories = this,
        repositoryFacade = repositoryFacade,
        mirrorService = mirrorService,
        resolutionProvider = resolutionProvider,
        statisticsFacade = statisticsFacade,
        extensions = extensions
    )

    private val repositories = AtomicReference(createRepositories(repositoriesSource.get()))

    init {
        repositoriesSource.subscribe { settings ->
            repositories.get().values.forEach { it.shutdown() }
            repositories.set(createRepositories(settings))
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

        val duplicatedNames =
            repositoriesConfiguration
                .groupingBy { it.id }
                .eachCount()
                .filterValues { it > 1 }
                .keys

        return repositoriesConfiguration
            .asSequence()
            .mapNotNull { configuration ->
                RepositoryIdentity
                    .create(configuration.id)
                    .filter(
                        { it.name !in duplicatedNames },
                        { "Repository name '${it.name}' is duplicated in Maven repository settings" }
                    )
                    .mapErr<Exception> { IllegalArgumentException(it) }
                    .flatMap { identity -> supplyThrowing { factory.createRepository(identity, configuration) } }
                    .onError {
                        failureFacade.throwException("Cannot load ${configuration.id} repository", it)
                    }
                    .orNull()
            }
            .associateBy { it.name }
    }

    fun getRepository(name: String): Repository? =
        repositories.get()[name]

    fun getRepositories(): Collection<Repository> =
        repositories.get().values

}
