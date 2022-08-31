/*
 * Copyright (c) 2022 dzikoysk
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

import com.reposilite.maven.RepositoryVisibility.HIDDEN
import com.reposilite.maven.RepositoryVisibility.PRIVATE
import com.reposilite.maven.RepositoryVisibility.PUBLIC
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.toErrorResponse
import com.reposilite.shared.unauthorizedError
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.RoutePermission
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.RoutePermission.WRITE
import io.javalin.http.HttpStatus.FORBIDDEN
import panda.std.Result

internal class RepositorySecurityProvider(private val accessTokenFacade: AccessTokenFacade) {

    fun canAccessRepository(accessToken: AccessTokenIdentifier?, repository: Repository): Boolean =
        when (repository.visibility) {
            PUBLIC -> true
            HIDDEN, PRIVATE -> accessToken?.let { accessTokenFacade.canSee(it, "/${repository.name}") } ?: false
        }

    fun canAccessResource(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<Unit, ErrorResponse> =
        when (repository.visibility) {
            PUBLIC -> Result.ok(Unit)
            HIDDEN -> Result.ok(Unit)
            PRIVATE -> hasPermissionTo(accessToken, repository, gav, READ)
        }

    fun canBrowseResource(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<Unit, ErrorResponse> =
        when (repository.visibility) {
            PUBLIC -> Result.ok(Unit)
            HIDDEN -> hasPermissionTo(accessToken, repository, gav, READ)
            PRIVATE -> hasPermissionTo(accessToken, repository, gav, READ)
        }

    fun canModifyResource(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Boolean =
        hasPermissionTo(accessToken, repository, gav, WRITE).isOk

    private fun hasPermissionTo(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location, permission: RoutePermission): Result<Unit, ErrorResponse> =
        accessToken
            ?.let {
                Result.`when`(accessTokenFacade.hasPermissionTo(accessToken, "/${repository.name}/$gav", permission),
                    { },
                    { FORBIDDEN.toErrorResponse("You must be the token owner or a manager to access this.") }
                )
            }
            ?: unauthorizedError("You need to provide credentials.")

}
