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

import com.reposilite.token.AccessToken
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.AccessTokenRepository
import com.reposilite.token.Route
import net.dzikoysk.exposed.shared.UNINITIALIZED_ENTITY_ID
import java.util.concurrent.atomic.AtomicInteger

internal class InMemoryAccessTokenRepository : AccessTokenRepository {

    private val tokens = mutableMapOf<Int, AccessToken>()
    private val permissions = mutableListOf<Pair<Int, AccessTokenPermission>>()
    private val routes = mutableListOf<Pair<Int, Route>>()
    private val id = AtomicInteger()

    override fun saveAccessToken(accessToken: AccessToken): AccessToken {
        val initializedAccessToken = when (accessToken.identifier.value) {
            UNINITIALIZED_ENTITY_ID -> accessToken.copy(identifier = AccessTokenIdentifier(value = id.incrementAndGet()))
            else -> accessToken
        }

        tokens[initializedAccessToken.identifier.value] = initializedAccessToken
        return initializedAccessToken
    }

    override fun deleteAccessToken(id: AccessTokenIdentifier) {
        tokens.remove(id.value)
    }

    override fun findAccessTokenById(id: AccessTokenIdentifier): AccessToken? =
        tokens[id.value]

    override fun findAccessTokenByName(name: String): AccessToken? =
        tokens.values.firstOrNull { it.name == name }

    override fun addPermission(id: AccessTokenIdentifier, permission: AccessTokenPermission) {
        permissions.add(Pair(id.value, permission))
    }

    override fun deletePermission(id: AccessTokenIdentifier, permission: AccessTokenPermission) {
        permissions.removeIf { (tokenId, associatedPermission) -> id.value == tokenId && permission == associatedPermission }
    }

    override fun findAccessTokenPermissionsById(id: AccessTokenIdentifier): Set<AccessTokenPermission> =
        permissions
            .filter { (tokenId) -> tokenId == id.value }
            .map { it.second }
            .toSet()

    override fun addRoute(id: AccessTokenIdentifier, route: Route) {
        routes.add(Pair(id.value, route))
    }

    override fun deleteRoute(id: AccessTokenIdentifier, route: Route) {
        routes.removeIf { it.second == route }
    }

    override fun findAccessTokenRoutesById(id: AccessTokenIdentifier): Set<Route> =
        routes
            .filter { it.first == id.value }
            .map { it.second }
            .toSet()

    override fun findAll(): Collection<AccessToken> =
        tokens.values

    override fun countAccessTokens(): Long =
        tokens.size.toLong()

}