package org.panda_lang.reposilite

import net.dzikoysk.dynamiclogger.backend.AggregatedLogger
import net.dzikoysk.dynamiclogger.slf4j.Slf4jLogger
import org.panda_lang.reposilite.auth.AuthService
import org.panda_lang.reposilite.auth.Authenticator
import org.panda_lang.reposilite.auth.TokenService
import org.panda_lang.reposilite.config.ConfigurationLoader
import org.panda_lang.reposilite.console.Console
import org.panda_lang.reposilite.error.FailureService
import org.panda_lang.reposilite.metadata.MetadataService
import org.panda_lang.reposilite.repository.*
import org.panda_lang.reposilite.resource.FrontendProvider
import org.panda_lang.reposilite.stats.StatsService
import org.panda_lang.reposilite.storage.FileSystemStorageProvider
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths

class ReposiliteFactory {

    fun createReposilite(configurationFile: Path, workingDirectory: Path, testEnv: Boolean): Reposilite {
        val configuration = ConfigurationLoader.tryLoad(configurationFile)

        val logger = AggregatedLogger(
            Slf4jLogger(LoggerFactory.getLogger(Reposilite::class.java))
        )

        val contextFactory = ReposiliteContextFactory(configuration.forwardedIp)
        val failureService = FailureService()

        val storageProvider = FileSystemStorageProvider.of(Paths.get(""), configuration.diskQuota)
        val tokenService = TokenService(workingDirectory, storageProvider)

        val repositoryService = RepositoryService()
        val authenticator = Authenticator(repositoryService, tokenService)
        val repositoryAuthenticator = RepositoryAuthenticator(configuration.rewritePathsEnabled, authenticator, repositoryService)
        val metadataService = MetadataService(failureService)

        return Reposilite(
            xlogger = logger,
            configuration = configuration,
            workingDirectory = workingDirectory,
            testEnv = testEnv,
            failureService = failureService,
            contextFactory = contextFactory,
            authenticator = authenticator,
            authService = AuthService(authenticator),
            repositoryAuthenticator = repositoryAuthenticator,
            storageProvider = storageProvider,
            repositoryService = repositoryService,
            tokenService = tokenService,
            metadataService = metadataService,
            deployService = DeployService(configuration.rewritePathsEnabled, authenticator, repositoryService, metadataService),
            lookupService = LookupService(repositoryAuthenticator, repositoryService),
            proxyService = ProxyService(
                configuration.storeProxied,
                configuration.proxyPrivate,
                configuration.proxyConnectTimeout,
                configuration.proxyReadTimeout,
                configuration.proxied,
                repositoryService,
                failureService,
                storageProvider
            ),
            statsService =  StatsService(workingDirectory, failureService, storageProvider),
            frontendService = FrontendProvider.load(configuration)
        )
    }

}