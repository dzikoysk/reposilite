package com.reposilite.maven.application

import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.journalist.Journalist
import com.reposilite.maven.LatestService
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.MavenService
import com.reposilite.maven.MetadataService
import com.reposilite.maven.ProxyService
import com.reposilite.maven.RepositoryProvider
import com.reposilite.maven.RepositorySecurityProvider
import com.reposilite.maven.RepositoryService
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.PluginComponents
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import com.reposilite.token.AccessTokenFacade
import panda.std.reactive.Reference
import java.nio.file.Path

internal class MavenComponents(
    private val workingDirectory: Path,
    private val journalist: Journalist,
    private val extensions: Extensions,
    private val remoteClientProvider: RemoteClientProvider,
    private val failureFacade: FailureFacade,
    private val storageFacade: StorageFacade,
    private val accessTokenFacade: AccessTokenFacade,
    private val statisticsFacade: StatisticsFacade,
    private val mavenSettings: Reference<MavenSettings>,
    private val frontendSettings: Reference<FrontendSettings>,
) : PluginComponents {

    private fun repositoryProvider(): RepositoryProvider =
        RepositoryProvider(
            workingDirectory = workingDirectory,
            remoteClientProvider = remoteClientProvider,
            failureFacade = failureFacade,
            storageFacade = storageFacade,
            repositoriesSource = mavenSettings.computed { it.repositories }
        )

    private fun securityProvider(): RepositorySecurityProvider =
        RepositorySecurityProvider(accessTokenFacade)

    private fun repositoryService(securityProvider: RepositorySecurityProvider): RepositoryService =
        RepositoryService(journalist, repositoryProvider(), securityProvider)

    private fun proxyService(): ProxyService =
        ProxyService(journalist)

    private fun metadataService(): MetadataService =
        MetadataService()

    private fun latestService(): LatestService =
        LatestService(frontendSettings.computed { it.id })

    private fun mavenService(repositoryService: RepositoryService, securityProvider: RepositorySecurityProvider, proxyService: ProxyService): MavenService =
        MavenService(
            journalist = journalist,
            repositoryService = repositoryService,
            repositorySecurityProvider = securityProvider,
            proxyService = proxyService,
            statisticsFacade = statisticsFacade,
            extensions = extensions
        )

    fun mavenFacade(
        securityProvider: RepositorySecurityProvider = securityProvider(),
        repositoryService: RepositoryService = repositoryService(securityProvider),
        proxyService: ProxyService = proxyService(),
        mavenService: MavenService = mavenService(repositoryService, securityProvider, proxyService),
        metadataService: MetadataService = metadataService(),
        latestService: LatestService = latestService()
    ): MavenFacade =
        MavenFacade(
            journalist = journalist,
            repositorySecurityProvider = securityProvider,
            repositoryService = repositoryService,
            mavenService = mavenService,
            metadataService = metadataService,
            latestService = latestService
        )

}