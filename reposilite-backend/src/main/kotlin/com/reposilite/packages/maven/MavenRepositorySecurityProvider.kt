/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.packages.maven

import com.reposilite.packages.maven.MavenRepositoryVisibility.HIDDEN
import com.reposilite.packages.maven.MavenRepositoryVisibility.PRIVATE
import com.reposilite.packages.maven.MavenRepositoryVisibility.PUBLIC
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

internal class MavenRepositorySecurityProvider(private val accessTokenFacade: AccessTokenFacade) {

    fun canAccessRepository(accessToken: AccessTokenIdentifier?, mavenRepository: MavenRepository): Boolean =
        when (mavenRepository.visibility) {
            PUBLIC -> true
            HIDDEN, PRIVATE -> accessToken?.let { accessTokenFacade.canSee(it, "/${mavenRepository.name}") } ?: false
        }

    fun canAccessResource(accessToken: AccessTokenIdentifier?, mavenRepository: MavenRepository, gav: Location): Result<Unit, ErrorResponse> =
        when (mavenRepository.visibility) {
            PUBLIC -> Result.ok(Unit)
            HIDDEN -> Result.ok(Unit)
            PRIVATE -> hasPermissionTo(accessToken, mavenRepository, gav, READ)
        }

    fun canBrowseResource(accessToken: AccessTokenIdentifier?, mavenRepository: MavenRepository, gav: Location): Result<Unit, ErrorResponse> =
        when (mavenRepository.visibility) {
            PUBLIC -> Result.ok(Unit)
            HIDDEN -> hasPermissionTo(accessToken, mavenRepository, gav, READ)
            PRIVATE -> hasPermissionTo(accessToken, mavenRepository, gav, READ)
        }

    fun canModifyResource(accessToken: AccessTokenIdentifier?, mavenRepository: MavenRepository, gav: Location): Boolean =
        hasPermissionTo(accessToken, mavenRepository, gav, WRITE).isOk

    private fun hasPermissionTo(accessToken: AccessTokenIdentifier?, mavenRepository: MavenRepository, gav: Location, permission: RoutePermission): Result<Unit, ErrorResponse> =
        accessToken
            ?.let {
                Result.`when`(accessTokenFacade.hasPermissionTo(accessToken, "/${mavenRepository.name}/$gav", permission),
                    { },
                    { FORBIDDEN.toErrorResponse("You must be the token owner or a manager to access this.") }
                )
            }
            ?: unauthorizedError("You need to provide credentials.")

}
