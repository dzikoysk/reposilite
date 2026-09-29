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
import com.reposilite.repository.api.RepositoryProvider
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.web.api.ReposiliteRoutes
import panda.std.Result
import panda.std.asError
import panda.std.ok

class RepositoryFacade internal constructor(
    private val accessResolver: RepositoryAccessResolver,
) : Facade {

    internal class Registration(
        val routes: ReposiliteRoutes,
        val provider: RepositoryProvider,
    )

    private val registrations = linkedMapOf<String, Registration>()

    fun register(
        type: String,
        routes: ReposiliteRoutes,
        provider: RepositoryProvider,
    ): Result<Unit, String> {
        val validationError = when {
            type.isBlank() -> "Repository type cannot be blank"
            type in registrations -> "Repository type '$type' is already registered"
            else -> routes.routes.firstNotNullOfOrNull { route ->
                when {
                    route.path != "/{repository}" && !route.path.startsWith("/{repository}/") -> "Repository type '$type' route '${route.path}' has to start with '/{repository}'"
                    route.methods.isEmpty() || route.methods.any { !it.isHttpMethod } -> "Repository type '$type' route '${route.path}' has to declare HTTP methods only"
                    else -> null
                }
            }
        }
        if (validationError != null) {
            return validationError.asError()
        }

        registrations[type] = Registration(routes, provider)
        return ok()
    }

    internal fun getRegistrations(): Map<String, Registration> =
        registrations.toMap()

    fun canAccessRepository(accessToken: AccessTokenIdentifier?, repository: RepositoryInfo): Boolean =
        accessResolver.canAccessRepository(accessToken, repository)

    fun canAccessResource(accessToken: AccessTokenIdentifier?, repository: RepositoryInfo, resourcePath: Location): Result<Unit, ErrorResponse> =
        accessResolver.canAccessResource(accessToken, repository, resourcePath)

    fun canBrowseResource(accessToken: AccessTokenIdentifier?, repository: RepositoryInfo, resourcePath: Location): Result<Unit, ErrorResponse> =
        accessResolver.canBrowseResource(accessToken, repository, resourcePath)

    fun canModifyResource(accessToken: AccessTokenIdentifier?, repository: RepositoryInfo, resourcePath: Location): Boolean =
        accessResolver.canModifyResource(accessToken, repository, resourcePath)
}
