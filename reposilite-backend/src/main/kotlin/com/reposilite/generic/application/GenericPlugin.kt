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

package com.reposilite.generic.application

import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.frontend.FrontendFacade
import com.reposilite.generic.GenericFacade
import com.reposilite.generic.GenericRepositoryProvider
import com.reposilite.generic.GenericRepositoryStore
import com.reposilite.generic.infrastructure.GenericEndpoints
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.parameters
import com.reposilite.repository.RepositoryFacade
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade

@Plugin(
    name = "generic",
    dependencies = ["failure", "local-configuration", "shared-configuration", "frontend", "storage", "repository"],
    settings = GenericSettings::class,
)
internal class GenericPlugin : ReposilitePlugin() {

    override fun initialize(): GenericFacade {
        val repositoryFacade = facade<RepositoryFacade>()
        val repositoryStore = GenericRepositoryStore(
            journalist = this,
            workingDirectory = parameters().workingDirectory,
            failureFacade = facade<FailureFacade>(),
            repositoryFacade = repositoryFacade,
            storageFacade = facade<StorageFacade>(),
            repositoriesSource = facade<SharedConfigurationFacade>()
                .getDomainSettings<GenericSettings>()
                .computed { it.repositories },
        )
        val genericFacade = GenericFacade(this, repositoryStore, repositoryFacade)

        repositoryFacade.registerProvider(
            GenericRepositoryProvider(
                genericFacade = genericFacade,
                routes = GenericEndpoints(
                    genericFacade = genericFacade,
                    frontendFacade = facade<FrontendFacade>(),
                    compressionStrategy = facade<LocalConfiguration>().compressionStrategy.get(),
                ),
            )
        )

        event { _: ReposiliteDisposeEvent -> repositoryStore.shutdown() }

        return genericFacade
    }
}
