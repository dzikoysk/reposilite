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

import com.reposilite.repository.api.RepositoryAccessMode.HIDDEN
import com.reposilite.repository.api.RepositoryAccessMode.PRIVATE
import com.reposilite.repository.api.RepositoryAccessMode.PUBLIC
import com.reposilite.repository.api.RepositoryDescriptor
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
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

internal class RepositoryAccessResolver(
    private val accessTokenFacade: AccessTokenFacade,
) {

    fun canAccessRepository(accessToken: AccessTokenIdentifier?, descriptor: RepositoryDescriptor): Boolean =
        when (descriptor.accessMode) {
            PUBLIC -> true
            HIDDEN, PRIVATE -> accessToken?.let { accessTokenFacade.canSee(it, "/${descriptor.name}") } ?: false
        }

    fun canAccessResource(accessToken: AccessTokenIdentifier?, descriptor: RepositoryDescriptor, resourcePath: Location): Result<Unit, ErrorResponse> =
        when {
            !resourcePath.isCanonicalResourcePath() -> badRequestError("Resource path has to be canonical")
            descriptor.accessMode == PUBLIC || descriptor.accessMode == HIDDEN -> Result.ok(Unit)
            else -> hasPermissionTo(accessToken, descriptor, resourcePath, READ)
        }

    fun canBrowseResource(accessToken: AccessTokenIdentifier?, descriptor: RepositoryDescriptor, resourcePath: Location): Result<Unit, ErrorResponse> =
        when {
            !resourcePath.isCanonicalResourcePath() -> badRequestError("Resource path has to be canonical")
            descriptor.accessMode == PUBLIC -> Result.ok(Unit)
            else -> hasPermissionTo(accessToken, descriptor, resourcePath, READ)
        }

    fun canModifyResource(accessToken: AccessTokenIdentifier?, descriptor: RepositoryDescriptor, resourcePath: Location): Boolean =
        resourcePath.isCanonicalResourcePath() && hasPermissionTo(accessToken, descriptor, resourcePath, WRITE).isOk

    private fun Location.isCanonicalResourcePath(): Boolean =
        toString().split('/').none { it == "." }

    private fun hasPermissionTo(
        accessToken: AccessTokenIdentifier?,
        descriptor: RepositoryDescriptor,
        resourcePath: Location,
        permission: RoutePermission,
    ): Result<Unit, ErrorResponse> =
        accessToken
            ?.let {
                Result.`when`(
                    accessTokenFacade.hasPermissionTo(accessToken, "/${descriptor.name}/$resourcePath", permission),
                    { },
                    { FORBIDDEN.toErrorResponse("You must be the token owner or a manager to access this.") }
                )
            }
            ?: unauthorizedError("You need to provide credentials.")
}
