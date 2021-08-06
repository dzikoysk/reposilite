package com.reposilite.maven

import com.reposilite.config.Configuration.RepositoryConfiguration
import com.reposilite.failure.FailureFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.shared.append
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.Route
import com.reposilite.token.api.RoutePermission
import net.dzikoysk.dynamiclogger.backend.InMemoryLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

@Suppress("LeakingThis")
internal abstract class MavenSpec {

    protected companion object {
        val UNAUTHORIZED: AccessToken? = null
    }

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

    fun createAccessToken(alias: String, secret: String, repository: String, gav: String, permission: RoutePermission): AccessToken {
        val routes = setOf(Route("/$repository/$gav", setOf(permission)))
        return AccessToken(alias = alias, secret = secret, routes = routes)
    }

}