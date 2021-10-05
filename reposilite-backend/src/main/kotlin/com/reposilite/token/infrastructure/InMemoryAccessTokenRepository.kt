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

package com.reposilite.token.infrastructure

import com.reposilite.token.AccessTokenRepository
import com.reposilite.token.api.AccessToken
import net.dzikoysk.exposed.shared.UNINITIALIZED_ENTITY_ID
import java.util.concurrent.atomic.AtomicInteger

internal class InMemoryAccessTokenRepository : AccessTokenRepository {

    private val tokens: MutableMap<Int, AccessToken> = HashMap(1)
    private val id = AtomicInteger()

    override fun saveAccessToken(accessToken: AccessToken): AccessToken {
        val initializedAccessToken = when (accessToken.id) {
            UNINITIALIZED_ENTITY_ID -> accessToken.copy(id = id.incrementAndGet())
            else -> accessToken
        }

        tokens[initializedAccessToken.id] = initializedAccessToken
        return initializedAccessToken
    }

    override fun deleteAccessToken(accessToken: AccessToken) {
        tokens.remove(accessToken.id)
    }

    override fun findAccessTokenByName(name: String): AccessToken? =
        tokens.values.firstOrNull { it.name == name }

    override fun findAll(): Collection<AccessToken> =
        tokens.values

    override fun countAccessTokens(): Long =
        tokens.size.toLong()

}