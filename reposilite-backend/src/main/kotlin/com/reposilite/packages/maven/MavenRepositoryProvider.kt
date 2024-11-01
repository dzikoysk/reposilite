/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.packages.maven

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.journalist.Journalist
import com.reposilite.packages.maven.application.RepositorySettings
import com.reposilite.plugin.Extensions
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.shared.notFoundError
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import java.nio.file.Path
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference

internal class MavenRepositoryProvider(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    private val remoteClientProvider: RemoteClientProvider,
    private val authenticationFacade: AuthenticationFacade,
    extensions: Extensions,
    private val failureFacade: FailureFacade,
    statisticsFacade: StatisticsFacade,
    private val storageFacade: StorageFacade,
    mirrorService: MirrorService,
    mavenRepositorySecurityProvider: MavenRepositorySecurityProvider,
    repositoriesSource: Reference<List<RepositorySettings>>,
) {

    val mavenRepositoryService = MavenRepositoryService(
        journalist = journalist,
        mavenRepositoryProvider = this,
        securityProvider = mavenRepositorySecurityProvider,
        mirrorService = mirrorService,
        statisticsFacade = statisticsFacade,
        extensions = extensions
    )

    private var repositories: Map<String, MavenRepository> = createRepositories(repositoriesSource.get())

    init {
        repositoriesSource.subscribe {
            repositories.forEach { (_, repository) -> repository.shutdown() }
            this.repositories = createRepositories(it)
        }
    }

    private fun createRepositories(repositoriesConfiguration: List<RepositorySettings>): Map<String, MavenRepository> {
        val factory = MavenRepositoryFactory(
            journalist = journalist,
            workingDirectory = workingDirectory,
            authenticationFacade = authenticationFacade,
            remoteClientProvider = remoteClientProvider,
            failureFacade = failureFacade,
            storageFacade = storageFacade,
            mavenRepositoryService = mavenRepositoryService,
            repositoriesNames = repositoriesConfiguration.map { it.id },
        )

        return repositoriesConfiguration.asSequence()
            .mapNotNull { configuration ->
                runCatching { factory.createRepository(configuration.id, configuration) }
                    .onFailure { failureFacade.throwException("Cannot load ${configuration.id} repository", it) }
                    .getOrNull()
            }
            .associateBy { it.name }
    }


    fun findRepository(name: String): Result<MavenRepository, ErrorResponse> =
        getRepository(name)
            ?.asSuccess()
            ?: notFoundError("Repository $name not found")

    fun getRepository(name: String): MavenRepository? =
        repositories[name]

    fun getRepositories(): Collection<MavenRepository> =
        repositories.values

}
