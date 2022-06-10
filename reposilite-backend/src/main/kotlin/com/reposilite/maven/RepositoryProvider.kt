/*
 * Copyright (c) 2022 dzikoysk
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

import com.reposilite.maven.application.RepositorySettings
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import panda.std.reactive.Reference
import java.nio.file.Path

internal class RepositoryProvider(
    private val workingDirectory: Path,
    private val remoteClientProvider: RemoteClientProvider,
    private val failureFacade: FailureFacade,
    private val storageFacade: StorageFacade,
    repositoriesSource: Reference<List<RepositorySettings>>,
) {

    private var repositories: Map<String, Repository>

    init {
        this.repositories = createRepositories(repositoriesSource.get())

        repositoriesSource.subscribe {
            repositories.forEach { (_, repository) -> repository.shutdown() }
            this.repositories = createRepositories(it)
        }
    }

    private fun createRepositories(repositoriesConfiguration: List<RepositorySettings>): Map<String, Repository> =
        RepositoryFactory(workingDirectory, remoteClientProvider, this, failureFacade, storageFacade, repositoriesConfiguration.map { it.id })
            .let { repositoriesConfiguration.associate { configuration -> configuration.id to it.createRepository(configuration.id, configuration) } }

    fun getRepositories(): Map<String, Repository> =
        repositories

}
