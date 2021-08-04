package org.panda_lang.reposilite.maven

import net.dzikoysk.dynamiclogger.backend.InMemoryLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.config.Configuration.RepositoryConfiguration
import org.panda_lang.reposilite.failure.FailureFacade
import org.panda_lang.reposilite.maven.api.LookupRequest
import org.panda_lang.reposilite.shared.append
import org.panda_lang.reposilite.token.api.AccessToken
import java.io.File
import java.nio.file.Files

@Suppress("LeakingThis")
internal abstract class MavenSpec {

    @TempDir
    @JvmField
    protected var workingDirectory: File? = null
    protected lateinit var mavenFacade: MavenFacade

    abstract fun repositories(): Map<String, RepositoryConfiguration>

    @BeforeEach
    private fun initializeFacade() {
        val logger = InMemoryLogger()
        val failureFacade = FailureFacade(logger)
        val metadataService = MetadataService(failureFacade)
        val repositorySecurityProvider = RepositorySecurityProvider()
        val repositoryService = RepositoryServiceFactory.createRepositoryService(logger, workingDirectory!!.toPath(), repositories())

        this.mavenFacade = MavenFacade(logger, metadataService, repositorySecurityProvider, repositoryService)
    }

    data class FileSpec(
        val repository: String,
        val gav: String,
        val content: String
    ) {

        fun toLookupRequest(authentication: AccessToken?): LookupRequest =
            LookupRequest(repository, gav, authentication)

    }

    fun addFileToRepository(fileSpec: FileSpec): FileSpec {
        workingDirectory!!.toPath()
            .resolve("repositories")
            .resolve(fileSpec.repository)
            .append(fileSpec.gav)
            .peek {
                Files.createDirectories(it.parent)
                Files.createFile(it)
                Files.write(it, fileSpec.content.toByteArray())
            }
            .get()

        return fileSpec
    }

}