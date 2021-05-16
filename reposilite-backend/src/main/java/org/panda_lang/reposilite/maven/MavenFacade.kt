package org.panda_lang.reposilite.maven

import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import org.panda_lang.reposilite.maven.repository.Repository
import org.panda_lang.reposilite.maven.repository.RepositoryService

class MavenFacade(
    internal val journalist: Journalist,
    internal val repositoryService: RepositoryService
) : Journalist {

    fun getRepositories(): Collection<Repository> = repositoryService.repositories

    override fun getLogger(): Logger = journalist.logger

}