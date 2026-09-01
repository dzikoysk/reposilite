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

package com.reposilite.repository

import com.reposilite.plugin.api.Facade
import com.reposilite.repository.api.ProvidedRepository
import com.reposilite.repository.api.Repository
import com.reposilite.repository.api.RepositoryProvider
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.web.api.ReposiliteRoutes
import panda.std.Result
import panda.std.reactive.Reference

class RepositoryFacade internal constructor(
    private val accessResolver: RepositoryAccessResolver,
) : Facade {

    private class RegisteredProvider(
        val provider: RepositoryProvider,
        val repositories: Reference<Collection<Repository>>,
    )

    private val providers = linkedMapOf<String, RegisteredProvider>()
    private var sealed = false

    fun registerProvider(provider: RepositoryProvider) {
        check(!sealed) { "Repository providers have to be registered before the HTTP server starts" }
        require(provider.id.isNotBlank()) { "Repository provider id cannot be blank" }
        require(provider.id !in providers) {
            "Repository provider '${provider.id}' is already registered"
        }

        providers[provider.id] = RegisteredProvider(provider, provider.repositories())
    }

    internal fun findRepositories(name: String): List<ProvidedRepository> =
        providers.values.flatMap { registeredProvider ->
            registeredProvider.repositories.get()
                .filter { it.name == name }
                .map { repository -> ProvidedRepository(registeredProvider.provider, repository) }
        }

    fun findRepository(name: String): ProvidedRepository? =
        findRepositories(name).singleOrNull()

    fun getRepositories(): Collection<ProvidedRepository> =
        providers.values
            .flatMap { registeredProvider ->
                registeredProvider.repositories.get().map { repository -> ProvidedRepository(registeredProvider.provider, repository) }
            }
            .groupBy { it.repository.name }
            .values
            .filter { it.size == 1 }
            .flatten()

    /** Validates one repository configuration before its provider initializes it. */
    fun validateRepositoryName(repositoryName: String) {
        require(
            repositoryName.isNotBlank() &&
                repositoryName == repositoryName.trim() &&
                repositoryName != "." &&
                repositoryName != ".." &&
                repositoryName.none { it == '/' || it == '\\' || it.isISOControl() }
        ) {
            "Repository name '$repositoryName' has to be a non-blank URL path segment"
        }
    }

    internal fun validateAndSeal(): Map<String, ReposiliteRoutes> {
        val providerRoutes = providers.mapValues { (_, registeredProvider) ->
            val provider = registeredProvider.provider
            provider.routes().also { routes ->
                routes.routes.forEach { route ->
                    require(route.path == "/{repository}" || route.path.startsWith("/{repository}/")) {
                        "Repository provider '${provider.id}' route '${route.path}' has to start with '/{repository}'"
                    }
                    require(route.methods.isNotEmpty() && route.methods.all { it.isHttpMethod }) {
                        "Repository provider '${provider.id}' route '${route.path}' has to declare HTTP methods only"
                    }
                }
            }
        }
        sealed = true
        return providerRoutes
    }

    fun canAccessRepository(accessToken: AccessTokenIdentifier?, repository: Repository): Boolean =
        accessResolver.canAccessRepository(accessToken, repository)

    fun canAccessResource(accessToken: AccessTokenIdentifier?, repository: Repository, resourcePath: Location): Result<Unit, ErrorResponse> =
        accessResolver.canAccessResource(accessToken, repository, resourcePath)

    fun canBrowseResource(accessToken: AccessTokenIdentifier?, repository: Repository, resourcePath: Location): Result<Unit, ErrorResponse> =
        accessResolver.canBrowseResource(accessToken, repository, resourcePath)

    fun canModifyResource(accessToken: AccessTokenIdentifier?, repository: Repository, resourcePath: Location): Boolean =
        accessResolver.canModifyResource(accessToken, repository, resourcePath)
}
