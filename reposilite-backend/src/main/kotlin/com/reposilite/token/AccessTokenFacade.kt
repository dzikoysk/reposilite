/*
 * Copyright (c) 2021 dzikoysk
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

import com.reposilite.plugin.api.Facade
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.AccessTokenSecurityProvider.generateSecret
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenResponse

class AccessTokenFacade internal constructor(
    private val temporaryRepository: AccessTokenRepository,
    private val persistentRepository: AccessTokenRepository
) : Facade {

    fun createAccessToken(request: CreateAccessTokenRequest): CreateAccessTokenResponse {
        val secret = request.secret ?: generateSecret()
        val encodedSecret = AccessTokenSecurityProvider.encodeSecret(secret)
        val accessToken = AccessToken(type = request.type, name = request.name, encryptedSecret = encodedSecret)

        return request.type.getRepository()
            .also { getAccessToken(accessToken.name)?.run { it.deleteAccessToken(this.id) } }
            .saveAccessToken(accessToken)
            .toDto()
            .let { CreateAccessTokenResponse(it, secret) }
    }

    fun secretMatches(id: AccessTokenId, secret: String): Boolean =
        getAccessTokenById(id)
            ?.let { AccessTokenSecurityProvider.matches(it.encryptedSecret, secret) }
            ?: false

    fun addRoute(accessToken: AccessTokenDto, route: Route) =
        accessToken.type.getRepository().addRoute(accessToken.id, route)

    fun addPermission(accessToken: AccessTokenDto, permission: AccessTokenPermission) =
        accessToken.type.getRepository().addPermission(accessToken.id, permission)

    fun hasPermissionTo(accessTokenDto: AccessTokenDto, toPath: String, requiredPermission: RoutePermission): Boolean =
        isManager(accessTokenDto) || accessTokenDto.type.getRepository()
            .findAccessTokenRoutesById(accessTokenDto.id)
            .hasPermissionTo(toPath, requiredPermission)

    private fun isManager(accessToken: AccessTokenDto): Boolean =
        accessToken.type.getRepository()
            .findAccessTokenPermissionsById(accessToken.id)
            .hasPermission(MANAGER)

    private fun updateToken(accessToken: AccessToken): AccessToken =
        accessToken.type.getRepository().saveAccessToken(accessToken)

    fun deleteToken(name: String) {
        getAccessToken(name)?.apply {
            type.getRepository().deleteAccessToken(this.id)
        }
    }

    private fun getAccessTokenById(id: AccessTokenId): AccessToken? =
        temporaryRepository.findAccessTokenById(id) ?: persistentRepository.findAccessTokenById(id)

    fun getAccessToken(name: String): AccessTokenDto? =
        (temporaryRepository.findAccessTokenByName(name) ?: persistentRepository.findAccessTokenByName(name))?.toDto()

    fun getAccessTokens(): Collection<AccessTokenDto> =
        (temporaryRepository.findAll() + persistentRepository.findAll()).map { it.toDto() }

    fun count(): Long =
        temporaryRepository.countAccessTokens() + persistentRepository.countAccessTokens()

    private fun AccessTokenType.getRepository(): AccessTokenRepository =
        if (this == PERSISTENT) persistentRepository else temporaryRepository

}