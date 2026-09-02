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
import com.reposilite.repository.api.RepositoryDescriptor
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.web.api.ReposiliteRoutes
import panda.std.Result
import panda.std.reactive.Reference

class RepositoryFacade internal constructor(
    private val accessResolver: RepositoryAccessResolver,
) : Facade {

    private class Registration(
        val routes: ReposiliteRoutes,
        val repositories: Reference<out Collection<RepositoryDescriptor>>,
    )

    private data class RegisteredRepository(
        val type: String,
        val descriptor: RepositoryDescriptor,
    )

    private val registrations = linkedMapOf<String, Registration>()
    private var sealed = false

    /** Registers the routes and live repositories of one repository type before HTTP startup. */
    fun register(
        type: String,
        routes: ReposiliteRoutes,
        repositories: Reference<out Collection<RepositoryDescriptor>>,
    ) {
        check(!sealed) { "Repository types have to be registered before the HTTP server starts" }
        require(type.isNotBlank()) { "Repository type cannot be blank" }
        require(type !in registrations) {
            "Repository type '$type' is already registered"
        }

        registrations[type] = Registration(routes, repositories)
    }

    private fun findRepositories(name: String): List<RegisteredRepository> =
        registrations.flatMap { (type, registration) ->
            registration.repositories.get()
                .filter { it.name == name }
                .map { repository -> RegisteredRepository(type, repository) }
        }

    internal fun findRepositoryTypes(name: String): List<String> =
        findRepositories(name).map { it.type }

    fun findRepository(name: String): RepositoryDescriptor? =
        findRepositories(name).singleOrNull()?.descriptor

    fun getRepositories(): Collection<RepositoryDescriptor> =
        registrations.flatMap { (type, registration) ->
            registration.repositories.get().map { repository -> RegisteredRepository(type, repository) }
        }
            .groupBy { it.descriptor.name }
            .values
            .filter { it.size == 1 }
            .map { it.single().descriptor }

    /** Validates a repository name before initialization. */
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
        val registeredRoutes = registrations.mapValues { (type, registration) ->
            registration.routes.also { routes ->
                routes.routes.forEach { route ->
                    require(route.path == "/{repository}" || route.path.startsWith("/{repository}/")) {
                        "Repository type '$type' route '${route.path}' has to start with '/{repository}'"
                    }
                    require(route.methods.isNotEmpty() && route.methods.all { it.isHttpMethod }) {
                        "Repository type '$type' route '${route.path}' has to declare HTTP methods only"
                    }
                }
            }
        }
        sealed = true
        return registeredRoutes
    }

    fun canAccessRepository(accessToken: AccessTokenIdentifier?, descriptor: RepositoryDescriptor): Boolean =
        accessResolver.canAccessRepository(accessToken, descriptor)

    fun canAccessResource(accessToken: AccessTokenIdentifier?, descriptor: RepositoryDescriptor, resourcePath: Location): Result<Unit, ErrorResponse> =
        accessResolver.canAccessResource(accessToken, descriptor, resourcePath)

    fun canBrowseResource(accessToken: AccessTokenIdentifier?, descriptor: RepositoryDescriptor, resourcePath: Location): Result<Unit, ErrorResponse> =
        accessResolver.canBrowseResource(accessToken, descriptor, resourcePath)

    fun canModifyResource(accessToken: AccessTokenIdentifier?, descriptor: RepositoryDescriptor, resourcePath: Location): Boolean =
        accessResolver.canModifyResource(accessToken, descriptor, resourcePath)
}
