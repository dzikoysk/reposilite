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
import com.reposilite.token.AccessTokenSecurityProvider.B_CRYPT_TOKENS_ENCODER
import com.reposilite.token.AccessTokenSecurityProvider.generateSecret
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.AccessTokenType
import com.reposilite.token.api.AccessTokenType.PERSISTENT
import com.reposilite.token.api.AccessTokenType.TEMPORARY
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenResponse

class AccessTokenFacade internal constructor(
    private val temporaryRepository: AccessTokenRepository,
    private val persistentRepository: AccessTokenRepository
) : Facade {

    fun createTemporaryAccessToken(request: CreateAccessTokenRequest): CreateAccessTokenResponse =
        createAccessToken(
            temporaryRepository,
            TEMPORARY,
            request.name,
            request.secret ?: generateSecret(),
            request.permissions
        )

    fun createAccessToken(request: CreateAccessTokenRequest): CreateAccessTokenResponse =
        createAccessToken(
            persistentRepository,
            PERSISTENT,
            request.name,
            request.secret ?: generateSecret(),
            request.permissions
        )

    private fun createAccessToken(
        repository: AccessTokenRepository,
        type: AccessTokenType,
        name: String,
        secret: String,
        permissions: Set<AccessTokenPermission>
    ): CreateAccessTokenResponse {
        val encodedSecret = B_CRYPT_TOKENS_ENCODER.encode(secret)
        val accessToken = AccessToken(type = type, name = name, encryptedSecret = encodedSecret, permissions = permissions)

        deleteToken(name)
        return CreateAccessTokenResponse(repository.saveAccessToken(accessToken), secret)
    }

    fun updateToken(accessToken: AccessToken): AccessToken =
        when (accessToken.type) {
            PERSISTENT -> persistentRepository.saveAccessToken(accessToken)
            TEMPORARY -> temporaryRepository.saveAccessToken(accessToken)
        }

    fun deleteToken(name: String): AccessToken? =
        deleteToken(temporaryRepository, name) ?: deleteToken(persistentRepository, name)

    private fun deleteToken(repository: AccessTokenRepository, name: String): AccessToken? =
        repository.findAccessTokenByName(name)?.also { persistentRepository.deleteAccessToken(it) }

    fun getToken(name: String): AccessToken? =
        temporaryRepository.findAccessTokenByName(name) ?: persistentRepository.findAccessTokenByName(name)

    fun getTokens(): Collection<AccessToken> =
        temporaryRepository.findAll() + persistentRepository.findAll()

    fun count(): Long =
        temporaryRepository.countAccessTokens() + persistentRepository.countAccessTokens()

}