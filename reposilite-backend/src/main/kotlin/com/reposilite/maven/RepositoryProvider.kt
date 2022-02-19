package com.reposilite.maven

import com.reposilite.settings.api.SharedConfiguration.RepositoryConfiguration
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.status.FailureFacade
import panda.std.reactive.Reference
import java.nio.file.Path

internal class RepositoryProvider(
    private val workingDirectory: Path,
    private val remoteClientProvider: RemoteClientProvider,
    private val failureFacade: FailureFacade,
    repositoriesSource: Reference<Map<String, RepositoryConfiguration>>,
) {

    private var repositories: Map<String, Repository>

    init {
        this.repositories = createRepositories(repositoriesSource.get())

        repositoriesSource.subscribe {
            repositories.forEach { (_, repository) -> repository.shutdown() }
            this.repositories = createRepositories(it)
        }
    }

    private fun createRepositories(repositoriesConfiguration: Map<String, RepositoryConfiguration>): Map<String, Repository> =
        RepositoryFactory(workingDirectory, remoteClientProvider, this, failureFacade, repositoriesConfiguration.keys)
            .let { repositoriesConfiguration.mapValues { (name, configuration) -> it.createRepository(name, configuration) } }

    fun getRepositories(): Map<String, Repository> =
        repositories

}