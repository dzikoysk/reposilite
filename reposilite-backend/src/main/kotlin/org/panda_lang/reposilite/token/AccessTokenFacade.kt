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
package org.panda_lang.reposilite.token

import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.api.AccessTokenPermission
import org.panda_lang.reposilite.token.api.CreateAccessTokenResponse
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.security.SecureRandom
import java.util.Base64

class AccessTokenFacade internal constructor(
    private val journalist: Journalist,
    private val accessTokenRepository: AccessTokenRepository
) : Journalist {

    companion object {
        val SECURE_RANDOM = SecureRandom()
        val B_CRYPT_TOKENS_ENCODER = BCryptPasswordEncoder()
    }

    fun createAccessToken(alias: String, permissions: Set<AccessTokenPermission> = emptySet()): CreateAccessTokenResponse {
        val randomBytes = ByteArray(48)
        SECURE_RANDOM.nextBytes(randomBytes)
        return createAccessToken(alias, Base64.getEncoder().encodeToString(randomBytes), permissions)
    }

    private fun createAccessToken(alias: String, token: String, permissions: Set<AccessTokenPermission>): CreateAccessTokenResponse {
        val encodedToken = B_CRYPT_TOKENS_ENCODER.encode(token)

        accessTokenRepository.saveAccessToken(AccessToken(alias = alias, secret = encodedToken, permissions = permissions))
        val accessToken = accessTokenRepository.findAccessTokenByAlias(alias)

        return CreateAccessTokenResponse(accessToken!!, token)
    }

    fun updateToken(accessToken: AccessToken) =
        accessTokenRepository.saveAccessToken(accessToken)

    fun deleteToken(alias: String): AccessToken? =
        accessTokenRepository.findAccessTokenByAlias(alias)
            ?.also { accessTokenRepository.deleteAccessToken(it) }

    fun getToken(alias: String): AccessToken? =
        accessTokenRepository.findAccessTokenByAlias(alias)

    fun getTokens(): Collection<AccessToken> =
        accessTokenRepository.findAll()

    fun count(): Long =
        accessTokenRepository.countAccessTokens()

    override fun getLogger(): Logger =
        journalist.logger

}