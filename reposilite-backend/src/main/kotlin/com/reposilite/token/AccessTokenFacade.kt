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

import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.CreateAccessTokenResponse
import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.security.SecureRandom
import java.util.Base64

class AccessTokenFacade internal constructor(
    private val journalist: Journalist,
    private val accessTokenRepository: AccessTokenRepository
) : Journalist {

    companion object {
        val B_CRYPT_TOKENS_ENCODER = BCryptPasswordEncoder()

        private val SECRET_GENERATOR: () -> String = {
            val secret = ByteArray(48)
            SecureRandom().nextBytes(secret)
            Base64.getEncoder().encodeToString(secret)
        }
    }

    fun createAccessToken(name: String, secret: String = SECRET_GENERATOR(), permissions: Set<AccessTokenPermission> = emptySet()): CreateAccessTokenResponse {
        val encodedToken = B_CRYPT_TOKENS_ENCODER.encode(secret)

        accessTokenRepository.saveAccessToken(AccessToken(name = name, secret = encodedToken, permissions = permissions))
        val accessToken = accessTokenRepository.findAccessTokenByName(name)

        return CreateAccessTokenResponse(accessToken!!, secret)
    }

    fun updateToken(accessToken: AccessToken) =
        accessTokenRepository.saveAccessToken(accessToken)

    fun deleteToken(name: String): AccessToken? =
        accessTokenRepository.findAccessTokenByName(name)
            ?.also { accessTokenRepository.deleteAccessToken(it) }

    fun getToken(name: String): AccessToken? =
        accessTokenRepository.findAccessTokenByName(name)

    fun getTokens(): Collection<AccessToken> =
        accessTokenRepository.findAll()

    fun count(): Long =
        accessTokenRepository.countAccessTokens()

    override fun getLogger(): Logger =
        journalist.logger

}