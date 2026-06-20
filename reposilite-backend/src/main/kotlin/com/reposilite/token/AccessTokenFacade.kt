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
package com.reposilite.token

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFoundError
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.AccessTokenSecurityProvider.generateSecret
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.api.AccessTokenDetails
import com.reposilite.token.api.AccessTokenDetailsDto
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenResponse
import com.reposilite.token.api.SecretType
import com.reposilite.token.api.SecretType.ENCRYPTED
import com.reposilite.token.api.SecretType.RAW
import com.reposilite.token.api.UpdateAccessTokenRequest
import panda.std.Result
import panda.std.asSuccess
import panda.std.letIf
import java.nio.file.Path
import java.time.LocalDate

class AccessTokenFacade internal constructor(
    private val journalist: Journalist,
    private val temporaryRepository: AccessTokenRepository,
    private val persistentRepository: AccessTokenRepository,
    private val exportService: ExportService
) : Facade, Journalist {

    fun createAccessToken(request: CreateAccessTokenRequest): CreateAccessTokenResponse {
        val existingToken = request.type.getRepository().findAccessTokenByName(request.name)
        val oppositeToken = request.type.getOppositeRepository().findAccessTokenByName(request.name)

        val secret = request.secret ?: generateSecret()
        val encodedSecret =
            when (request.secretType) {
                RAW -> AccessTokenSecurityProvider.encodeSecret(secret)
                ENCRYPTED -> secret
            }

        val createdToken =
            request.type.getRepository()
                .saveAccessToken(
                    AccessToken(
                        identifier = existingToken?.identifier ?: AccessTokenIdentifier(type = request.type),
                        name = request.name,
                        createdAt = LocalDate.now(),
                        encryptedSecret = encodedSecret,
                        description = request.description,
                        expiresAt = request.expiresAt,
                    )
                )
                .toDto()
        oppositeToken?.let { request.type.getOppositeRepository().deleteAccessToken(it.identifier) }

        val routes = syncPermissionsAndRoutes(
            identifier = createdToken.identifier,
            permissions = request.permissions,
            routeRequests = request.routes
        )

        return CreateAccessTokenResponse(
            accessToken = createdToken,
            permissions = request.permissions,
            secret = secret,
            routes = routes
        )
    }

    fun updateAccessToken(name: String, request: UpdateAccessTokenRequest): Result<AccessTokenDetailsDto, ErrorResponse> {
        val dto = getAccessToken(name) ?: return notFoundError("Token not found")

        getRawAccessTokenById(dto.identifier)
            ?.copy(description = request.description, expiresAt = request.expiresAt)
            ?.let { dto.identifier.type.getRepository().saveAccessToken(it) }

        syncPermissionsAndRoutes(dto.identifier, request.permissions, request.routes)

        return getAccessTokenDetails(getAccessToken(name)!!).asSuccess()
    }

    private fun syncPermissionsAndRoutes(
        identifier: AccessTokenIdentifier,
        permissions: Set<AccessTokenPermission>,
        routeRequests: Set<CreateAccessTokenRequest.Route>,
    ): Set<Route> {
        val currentPermissions = getPermissions(identifier)
        (currentPermissions - permissions).forEach { deletePermission(identifier, it) }
        (permissions - currentPermissions).forEach { addPermission(identifier, it) }

        val routes = routeRequests.flatMapTo(HashSet()) { route -> route.permissions.map { Route(route.path, it) } }
        val currentRoutes = getRoutes(identifier)
        (currentRoutes - routes).forEach { deleteRoute(identifier, it) }
        (routes - currentRoutes).forEach { addRoute(identifier, it) }
        return routes
    }

    fun addAccessToken(accessTokenDetails: AccessTokenDetails): AccessTokenDto =
        with (accessTokenDetails) {
            val (accessTokenDto) = createAccessToken(
                CreateAccessTokenRequest(
                    type = accessToken.identifier.type,
                    name = accessToken.name,
                    secretType = ENCRYPTED,
                    secret = accessToken.encryptedSecret,
                    description = accessToken.description
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

    fun exportToFile(toFile: Path): Path =
        getAccessTokens()
            .map { getAccessTokenDetailsById(it.identifier)!! }
            .let { exportService.exportToFile(it, toFile) }

    fun importFromFile(fromFile: Path): Result<Collection<AccessTokenDetails>, Exception> =
        exportService.importFromFile(fromFile)

    fun secretMatches(id: AccessTokenIdentifier, secret: String): Boolean =
        getRawAccessTokenById(id)
            ?.let { AccessTokenSecurityProvider.matches(it.encryptedSecret, secret) }
            ?: false

    fun addPermission(identifier: AccessTokenIdentifier, permission: AccessTokenPermission): AccessTokenPermission =
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
            .any { it.path.startsWith(pathFragment, ignoreCase = true) || pathFragment.startsWith(it.path) }

    fun deletePermission(identifier: AccessTokenIdentifier, permission: AccessTokenPermission) =
        identifier.type.getRepository().deletePermission(identifier, permission)

    fun getPermissions(identifier: AccessTokenIdentifier): Set<AccessTokenPermission> =
        identifier.type.getRepository().findAccessTokenPermissionsById(identifier)

    fun addRoute(identifier: AccessTokenIdentifier, route: Route): Route =
        identifier.type.getRepository().addRoute(identifier, route)

    fun deleteRoute(identifier: AccessTokenIdentifier, route: Route) =
        identifier.type.getRepository().deleteRoute(identifier, route)

    fun deleteRoutesByPath(identifier: AccessTokenIdentifier, path: String) =
        identifier.type.getRepository().deleteRoutesByPath(identifier, path)

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

    fun regenerateAccessToken(accessTokenDto: AccessTokenDto, secret: String?, secretType: SecretType = RAW): Result<String, ErrorResponse> {
        val rawSecret = secret ?: generateSecret()
        val encodedSecret = rawSecret.letIf(secretType == RAW) { AccessTokenSecurityProvider.encodeSecret(it) } // encode if not already encoded

        return getRawAccessTokenById(accessTokenDto.identifier)
            ?.copy(encryptedSecret = encodedSecret)
            ?.let { it.identifier.type.getRepository().saveAccessToken(it) }
            ?.let { rawSecret.asSuccess() }
            ?: return notFoundError("Token not found")
    }

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

    fun getAccessTokenDetails(token: AccessTokenDto): AccessTokenDetailsDto =
        AccessTokenDetailsDto.of(token, getPermissions(token.identifier), getRoutes(token.identifier))

    fun getAccessTokensDetails(): Collection<AccessTokenDetailsDto> =
        getAccessTokens().map { getAccessTokenDetails(it) }

    fun count(): Long =
        temporaryRepository.countAccessTokens() + persistentRepository.countAccessTokens()

    private fun AccessTokenType.getRepository(): AccessTokenRepository =
        if (this == PERSISTENT) persistentRepository else temporaryRepository

    private fun AccessTokenType.getOppositeRepository(): AccessTokenRepository =
        if (this == PERSISTENT) temporaryRepository else persistentRepository

    override fun getLogger(): Logger =
        journalist.logger

}
