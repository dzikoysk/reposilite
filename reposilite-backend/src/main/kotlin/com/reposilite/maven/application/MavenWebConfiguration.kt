/*
 * Copyright (c) 2021 dzikoysk
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

import com.reposilite.Reposilite
import com.reposilite.journalist.Journalist
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.MetadataService
import com.reposilite.maven.ProxyService
import com.reposilite.maven.RepositoryProvider
import com.reposilite.maven.RepositorySecurityProvider
import com.reposilite.maven.RepositoryService
import com.reposilite.maven.infrastructure.MavenApiEndpoints
import com.reposilite.maven.infrastructure.MavenEndpoints
import com.reposilite.maven.infrastructure.MavenLatestApiEndpoints
import com.reposilite.settings.api.SharedConfiguration.RepositoryConfiguration
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.web.WebConfiguration
import com.reposilite.web.application.ReposiliteRoutes
import panda.std.reactive.Reference
import java.nio.file.Path

internal object MavenWebConfiguration : WebConfiguration {

    fun createFacade(
        journalist: Journalist,
        workingDirectory: Path,
        remoteClientProvider: RemoteClientProvider,
        repositories: Reference<Map<String, RepositoryConfiguration>>,
        statisticsFacade: StatisticsFacade
    ): MavenFacade {
        val repositoryProvider = RepositoryProvider(journalist, workingDirectory, remoteClientProvider, repositories)
        val securityProvider = RepositorySecurityProvider()
        val repositoryService = RepositoryService(journalist, repositoryProvider, securityProvider)

        return MavenFacade(
            journalist,
            securityProvider,
            repositoryService,
            ProxyService(),
            MetadataService(repositoryService),
            statisticsFacade
        )
    }

    override fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> = setOf(
        MavenEndpoints(reposilite.mavenFacade, reposilite.frontendFacade, reposilite.settingsFacade),
        MavenApiEndpoints(reposilite.mavenFacade),
        MavenLatestApiEndpoints(reposilite.mavenFacade)
    )

    override fun dispose(reposilite: Reposilite) {
        reposilite.mavenFacade.getRepositories().forEach {
            it.shutdown()
        }
    }

}