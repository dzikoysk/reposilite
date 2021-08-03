package org.panda_lang.reposilite.maven

import net.dzikoysk.dynamiclogger.backend.InMemoryLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.config.Configuration.RepositoryConfiguration
import org.panda_lang.reposilite.failure.FailureFacade
import java.io.File

@Suppress("LeakingThis")
internal abstract class MavenSpec {

    @TempDir
    @JvmField
    var workingDirectory: File? = null

    lateinit var mavenFacade: MavenFacade

    @BeforeEach
    private fun initializeFacade() {
        val logger = InMemoryLogger()
        val failureFacade = FailureFacade(logger)
        val metadataService = MetadataService(failureFacade)
        val repositorySecurityProvider = RepositorySecurityProvider()
        val repositoryService = RepositoryServiceFactory.createRepositoryService(logger, workingDirectory!!.toPath(), repositories())

        this.mavenFacade = MavenFacade(logger, metadataService, repositorySecurityProvider, repositoryService)
    }

    protected abstract fun repositories(): Map<String, RepositoryConfiguration>

}