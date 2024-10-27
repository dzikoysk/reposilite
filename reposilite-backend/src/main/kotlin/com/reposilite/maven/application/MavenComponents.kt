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

package com.reposilite.maven.application

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.journalist.Journalist
import com.reposilite.maven.LatestService
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.MetadataService
import com.reposilite.maven.MirrorService
import com.reposilite.maven.RepositoryProvider
import com.reposilite.maven.RepositorySecurityProvider
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.PluginComponents
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import com.reposilite.token.AccessTokenFacade
import panda.std.reactive.Reference
import java.nio.file.Path
import java.time.Clock

internal class MavenComponents(
    private val clock: Clock,
    private val workingDirectory: Path,
    private val journalist: Journalist,
    private val extensions: Extensions,
    private val remoteClientProvider: RemoteClientProvider,
    private val failureFacade: FailureFacade,
    private val storageFacade: StorageFacade,
    private val authenticationFacade: AuthenticationFacade,
    private val accessTokenFacade: AccessTokenFacade,
    private val statisticsFacade: StatisticsFacade,
    private val mavenSettings: Reference<MavenSettings>,
    private val id: Reference<String>,
) : PluginComponents {

    private fun securityProvider(): RepositorySecurityProvider =
        RepositorySecurityProvider(accessTokenFacade)

    private fun metadataService(): MetadataService =
        MetadataService(securityProvider())

    private fun mirrorService(): MirrorService =
        MirrorService(
            journalist = journalist,
            clock = clock
        )

    private fun repositoryProvider(
        mirrorService: MirrorService = mirrorService(),
        securityProvider: RepositorySecurityProvider = securityProvider(),
    ): RepositoryProvider =
        RepositoryProvider(
            journalist = journalist,
            workingDirectory = workingDirectory,
            remoteClientProvider = remoteClientProvider,
            authenticationFacade = authenticationFacade,
            failureFacade = failureFacade,
            storageFacade = storageFacade,
            mirrorService = mirrorService,
            statisticsFacade = statisticsFacade,
            extensions = extensions,
            repositorySecurityProvider = securityProvider,
            repositoriesSource = mavenSettings.computed { it.repositories }
        )

    private fun latestService(): LatestService =
        LatestService(id)

    fun mavenFacade(
        securityProvider: RepositorySecurityProvider = securityProvider(),
        metadataService: MetadataService = metadataService(),
        latestService: LatestService = latestService(),
        repositoryProvider: RepositoryProvider = repositoryProvider(),
    ): MavenFacade =
        MavenFacade(
            journalist = journalist,
            repositorySecurityProvider = securityProvider,
            repositoryProvider = repositoryProvider,
            metadataService = metadataService,
            latestService = latestService
        )

}
