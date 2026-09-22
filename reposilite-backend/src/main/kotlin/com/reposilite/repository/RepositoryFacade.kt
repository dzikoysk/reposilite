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
import com.reposilite.repository.api.RepositoryInfo
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.web.api.ReposiliteRoutes
import panda.std.Result

class RepositoryFacade internal constructor(
    private val accessResolver: RepositoryAccessResolver,
) : Facade {

    private class Registration(
        val routes: ReposiliteRoutes,
        val repositories: () -> Collection<RepositoryInfo>,
    )

    private val registrations = linkedMapOf<String, Registration>()
    private var sealed = false

    /** Registers the routes and live repositories of one repository type before HTTP startup. */
    fun register(
        type: String,
        routes: ReposiliteRoutes,
        repositories: () -> Collection<RepositoryInfo>,
    ) {
        check(!sealed) { "Repository types have to be registered before the HTTP server starts" }
        require(type.isNotBlank()) { "Repository type cannot be blank" }
        require(type !in registrations) {
            "Repository type '$type' is already registered"
        }

        registrations[type] = Registration(routes, repositories)
    }

    internal fun findRepositoryTypes(name: String): List<String> =
        registrations.flatMap { (type, registration) ->
            registration.repositories()
                .filter { it.name == name }
                .map { type }
        }

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

    fun canAccessRepository(accessToken: AccessTokenIdentifier?, repository: RepositoryInfo): Boolean =
        accessResolver.canAccessRepository(accessToken, repository)

    fun canAccessResource(accessToken: AccessTokenIdentifier?, repository: RepositoryInfo, resourcePath: Location): Result<Unit, ErrorResponse> =
        accessResolver.canAccessResource(accessToken, repository, resourcePath)

    fun canBrowseResource(accessToken: AccessTokenIdentifier?, repository: RepositoryInfo, resourcePath: Location): Result<Unit, ErrorResponse> =
        accessResolver.canBrowseResource(accessToken, repository, resourcePath)

    fun canModifyResource(accessToken: AccessTokenIdentifier?, repository: RepositoryInfo, resourcePath: Location): Boolean =
        accessResolver.canModifyResource(accessToken, repository, resourcePath)
}
