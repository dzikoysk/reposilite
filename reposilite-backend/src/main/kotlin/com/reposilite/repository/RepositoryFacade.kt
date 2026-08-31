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

class RepositoryFacade internal constructor(
    private val accessResolver: RepositoryAccessResolver,
) : Facade {

    private val providers = linkedMapOf<String, RepositoryProvider>()
    private var sealed = false

    fun registerProvider(provider: RepositoryProvider) {
        check(!sealed) { "Repository providers have to be registered before the HTTP server starts" }
        require(provider.id.isNotBlank()) { "Repository provider id cannot be blank" }
        require(providers.putIfAbsent(provider.id, provider) == null) {
            "Repository provider '${provider.id}' is already registered"
        }
    }

    fun findRepository(name: String): ProvidedRepository? {
        val matches = providers.values.mapNotNull { provider ->
            provider.findRepository(name)?.let { repository -> ProvidedRepository(provider, repository) }
        }

        require(matches.size <= 1) {
            "Repository '$name' is provided by multiple providers: ${matches.joinToString { it.provider.id }}"
        }

        return matches.singleOrNull()
    }

    fun getRepositories(): Collection<ProvidedRepository> {
        val repositories = providers.values.flatMap { provider ->
            provider.getRepositories().map { repository -> ProvidedRepository(provider, repository) }
        }
        val duplicatedNames = repositories.groupBy { it.repository.name }.filterValues { it.size > 1 }

        require(duplicatedNames.isEmpty()) {
            duplicatedNames.entries.joinToString(
                prefix = "Repository names have to be unique across providers: ",
                transform = { (name, matches) -> "$name (${matches.joinToString { it.provider.id }})" }
            )
        }

        return repositories
    }

    /**
     * Validates one repository configuration before its provider initializes it.
     * Providers should report the exception and skip only the rejected configuration.
     */
    fun validateRepositoryName(providerId: String, repositoryName: String) {
        require(
            repositoryName.isNotBlank() &&
                repositoryName == repositoryName.trim() &&
                repositoryName != "." &&
                repositoryName != ".." &&
                repositoryName.none { it == '/' || it == '\\' || it.isISOControl() }
        ) {
            "Repository name '$repositoryName' has to be a non-blank URL path segment"
        }

        val owner = providers.values
            .asSequence()
            .filter { it.id != providerId }
            .firstOrNull { it.findRepository(repositoryName) != null }

        require(owner == null) {
            "Repository '$repositoryName' is already provided by '${owner?.id}'"
        }
    }

    internal fun validateAndSeal(): Map<RepositoryProvider, ReposiliteRoutes> {
        val providerRoutes = providers.values.associateWith { provider ->
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
