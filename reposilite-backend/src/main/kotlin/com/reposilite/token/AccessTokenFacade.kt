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
package com.reposilite.token

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.AccessTokenSecurityProvider.generateSecret
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.api.AccessTokenDetails
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenResponse
import com.reposilite.token.api.SecretType.ENCRYPTED
import com.reposilite.token.api.SecretType.RAW
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFoundError
import panda.std.Result
import panda.std.asSuccess
import panda.std.letIf

class AccessTokenFacade internal constructor(
    private val journalist: Journalist,
    private val temporaryRepository: AccessTokenRepository,
    private val persistentRepository: AccessTokenRepository
) : Facade, Journalist {

    fun createAccessToken(request: CreateAccessTokenRequest): CreateAccessTokenResponse {
        val secret = request.secret ?: generateSecret()
        val encodedSecret = secret.letIf(request.secretType == RAW) { AccessTokenSecurityProvider.encodeSecret(it) }
        val accessToken = AccessToken(identifier = AccessTokenIdentifier(type = request.type), name = request.name, encryptedSecret = encodedSecret)

        return request.type.getRepository()
            .also { getAccessToken(accessToken.name)?.run { it.deleteAccessToken(this.identifier) } }
            .saveAccessToken(accessToken)
            .toDto()
            .let { CreateAccessTokenResponse(it, secret) }
    }

    fun addAccessToken(accessTokenDetails: AccessTokenDetails): AccessTokenDto =
        with (accessTokenDetails) {
            val (accessTokenDto) = createAccessToken(
                CreateAccessTokenRequest(
                    type = accessToken.identifier.type,
                    name = accessToken.name,
                    secretType = ENCRYPTED,
                    secret = accessToken.encryptedSecret
                )
            )

            permissions.forEach {
                addPermission(accessTokenDto.identifier, it)
            }

            routes.forEach {
                addRoute(accessTokenDto.identifier, Route(it.path, it.permission))
            }

            accessTokenDto
        }

    fun secretMatches(id: AccessTokenIdentifier, secret: String): Boolean =
        getRawAccessTokenById(id)
            ?.let { AccessTokenSecurityProvider.matches(it.encryptedSecret, secret) }
            ?: false

    fun addPermission(identifier: AccessTokenIdentifier, permission: AccessTokenPermission) =
        identifier.type.getRepository().addPermission(identifier, permission)

    fun hasPermission(identifier: AccessTokenIdentifier, permission: AccessTokenPermission): Boolean =
        identifier.type.getRepository().findAccessTokenPermissionsById(identifier).contains(permission)

    fun hasPermissionTo(identifier: AccessTokenIdentifier, toPath: String, requiredPermission: RoutePermission): Boolean =
        hasPermission(identifier, MANAGER) || identifier.type.getRepository()
            .findAccessTokenRoutesById(identifier)
            .any { it.hasPermissionTo(toPath, requiredPermission) }

    fun canSee(identifier: AccessTokenIdentifier, pathFragment: String): Boolean =
        hasPermission(identifier, MANAGER) || identifier.type.getRepository()
            .findAccessTokenRoutesById(identifier)
            .any { it.path.startsWith(pathFragment, ignoreCase = true) }

    fun deletePermission(identifier: AccessTokenIdentifier, permission: AccessTokenPermission) =
        identifier.type.getRepository().deletePermission(identifier, permission)

    fun getPermissions(identifier: AccessTokenIdentifier): Set<AccessTokenPermission> =
        identifier.type.getRepository().findAccessTokenPermissionsById(identifier)

    fun addRoute(identifier: AccessTokenIdentifier, route: Route) =
        identifier.type.getRepository().addRoute(identifier, route)

    fun deleteRoute(identifier: AccessTokenIdentifier, route: Route) =
        identifier.type.getRepository().deleteRoute(identifier, route)

    fun getRoutes(id: AccessTokenIdentifier): Set<Route> =
        id.type.getRepository().findAccessTokenRoutesById(id)

    fun updateToken(updatedToken: AccessTokenDto): AccessTokenDto? =
        getRawAccessTokenById(updatedToken.identifier)
            ?.copy(
                name = updatedToken.name,
                createdAt = updatedToken.createdAt,
                description = updatedToken.description
            )
            ?.let { it.identifier.type.getRepository().saveAccessToken(it) }
            ?.toDto()

    fun deleteToken(id: AccessTokenIdentifier): Result<Unit, ErrorResponse> =
        getRawAccessTokenById(id)
            ?.let { it.identifier.type.getRepository().deleteAccessToken(it.identifier).asSuccess() }
            ?: notFoundError("Token not found")

    private fun getRawAccessTokenById(id: AccessTokenIdentifier): AccessToken? =
        id.type.getRepository().findAccessTokenById(id)

    fun getAccessTokenById(id: AccessTokenIdentifier): AccessTokenDto? =
        getRawAccessTokenById(id)?.toDto()

    internal fun getAccessTokenDetailsById(id: AccessTokenIdentifier): AccessTokenDetails? =
        getRawAccessTokenById(id)?.let { AccessTokenDetails(it, getPermissions(id), getRoutes(id)) }

    fun getAccessToken(name: String): AccessTokenDto? =
        (temporaryRepository.findAccessTokenByName(name) ?: persistentRepository.findAccessTokenByName(name))?.toDto()

    fun getAccessTokens(): Collection<AccessTokenDto> =
        (temporaryRepository.findAll() + persistentRepository.findAll()).map { it.toDto() }

    fun count(): Long =
        temporaryRepository.countAccessTokens() + persistentRepository.countAccessTokens()

    private fun AccessTokenType.getRepository(): AccessTokenRepository =
        if (this == PERSISTENT) persistentRepository else temporaryRepository

    override fun getLogger(): Logger =
        journalist.logger

}