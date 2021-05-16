/*
 * Copyright (c) 2020 Dzikoysk
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
import org.panda_lang.utilities.commons.collection.Pair
import org.panda_lang.utilities.commons.function.Option
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.security.SecureRandom
import java.util.*

class AccessTokenFacade internal constructor(
    private val journalist: Journalist,
    private val accessTokenRepository: AccessTokenRepository
) : Journalist {

    companion object {
        val SECURE_RANDOM = SecureRandom()
        val B_CRYPT_TOKENS_ENCODER = BCryptPasswordEncoder()
    }

    fun createAccessToken(alias: String): Pair<String, AccessToken> {
        val randomBytes = ByteArray(48)
        SECURE_RANDOM.nextBytes(randomBytes)
        return createAccessToken(alias, Base64.getEncoder().encodeToString(randomBytes))
    }

    private fun createAccessToken(alias: String, token: String): Pair<String, AccessToken> {
        val encodedToken = B_CRYPT_TOKENS_ENCODER.encode(token)

        accessTokenRepository.createAccessToken(AccessToken(alias = alias, secret = encodedToken))
        val accessToken = accessTokenRepository.findAccessTokenByAlias(alias)

        return Pair(token, accessToken)
    }

    fun deleteToken(alias: String) =
        accessTokenRepository.deleteAccessTokenByAlias(alias)

    fun getToken(alias: String): Option<AccessToken> =
        Option.of(accessTokenRepository.findAccessTokenByAlias(alias))

    fun getTokens(): Collection<AccessToken> =
        accessTokenRepository.findAll()

    fun count(): Long =
        accessTokenRepository.countAccessTokens()

    override fun getLogger(): Logger =
        journalist.logger

}