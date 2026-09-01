/*
 * Copyright (c) 2020-2026 dzikoysk
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

import com.reposilite.frontend.FrontendFacade
import com.reposilite.maven.infrastructure.MavenEndpoints
import com.reposilite.repository.api.Repository as RepositoryApi
import com.reposilite.repository.api.RepositoryProvider
import com.reposilite.web.api.ReposiliteRoutes
import panda.std.reactive.Reference

internal class MavenRepositoryProvider(
    private val mavenFacade: MavenFacade,
    private val frontendFacade: FrontendFacade,
    private val compressionStrategy: String,
) : RepositoryProvider {

    override val id: String = MAVEN_REPOSITORY_PROVIDER_ID

    override fun routes(): ReposiliteRoutes =
        MavenEndpoints(mavenFacade, frontendFacade, compressionStrategy)

    override fun repositories(): Reference<Collection<RepositoryApi>> =
        mavenFacade.repositories()
}

internal const val MAVEN_REPOSITORY_PROVIDER_ID = "maven"
