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

package com.reposilite.repository.application

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.repository.RepositoryAccessResolver
import com.reposilite.repository.RepositoryFacade
import com.reposilite.repository.infrastructure.RepositoryEndpointRouter
import com.reposilite.status.FailureFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.web.api.HttpServerInitializationEvent
import com.reposilite.web.infrastructure.createReposiliteEndpointFactory
import io.javalin.config.JavalinState
import io.javalin.plugin.Plugin as JavalinPlugin

@Plugin(name = "repository", dependencies = ["web", "failure", "access-token", "authentication"])
class RepositoryPlugin : ReposilitePlugin() {
    override fun initialize(): RepositoryFacade {
        val repositoryFacade = RepositoryFacade(
            accessResolver = RepositoryAccessResolver(facade<AccessTokenFacade>()),
        )

        event { event: HttpServerInitializationEvent ->
            val providerRoutes = repositoryFacade.validateAndSeal()

            val repositoryEndpointRouter = RepositoryEndpointRouter(
                repositoryFacade = repositoryFacade,
                providerRoutes = providerRoutes,
                endpointFactory = createReposiliteEndpointFactory(
                    journalist = this,
                    failureFacade = facade<FailureFacade>(),
                    accessTokenFacade = facade<AccessTokenFacade>(),
                    authenticationFacade = facade<AuthenticationFacade>(),
                ),
                routerConfig = event.config.router,
            )

            // Keep the repository gateway behind regular routes because its paths intentionally match broadly.
            event.config.registerPlugin(object : JavalinPlugin<Unit?>() {
                override fun onStart(state: JavalinState) {
                    repositoryEndpointRouter.gatewayEndpoints.forEach { endpoint ->
                        state.routes.addEndpoint(endpoint)
                    }
                }
            })
        }

        return repositoryFacade
    }
}
