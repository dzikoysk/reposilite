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

package com.reposilite.token.infrastructure

import com.reposilite.token.AccessToken
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.AccessTokenRepository
import com.reposilite.token.AccessTokenType.TEMPORARY
import com.reposilite.token.Route
import net.dzikoysk.exposed.shared.UNINITIALIZED_ENTITY_ID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

internal class InMemoryAccessTokenRepository : AccessTokenRepository {

    private val tokens = ConcurrentHashMap<Int, AccessToken>()
    private val permissions = CopyOnWriteArrayList<Pair<Int, AccessTokenPermission>>()
    private val routes = CopyOnWriteArrayList<Pair<Int, Route>>()
    private val id = AtomicInteger()

    override fun saveAccessToken(accessToken: AccessToken): AccessToken {
        val initializedAccessToken = when (accessToken.identifier.value) {
            UNINITIALIZED_ENTITY_ID -> accessToken.copy(identifier = AccessTokenIdentifier(type = TEMPORARY, value = id.incrementAndGet()))
            else -> accessToken
        }

        tokens[initializedAccessToken.identifier.value] = initializedAccessToken
        return initializedAccessToken
    }

    override fun deleteAccessToken(id: AccessTokenIdentifier) {
        tokens.remove(id.value)
    }

    override fun findAccessTokenById(id: AccessTokenIdentifier): AccessToken? {
        val token = tokens[id.value] ?: return null
        if (token.isExpired()) {
            evict(token.identifier)
            return null
        }
        return token
    }

    override fun findAccessTokenByName(name: String): AccessToken? {
        val token = tokens.values.firstOrNull { it.name == name } ?: return null
        if (token.isExpired()) {
            evict(token.identifier)
            return null
        }
        return token
    }

    override fun addPermission(id: AccessTokenIdentifier, permission: AccessTokenPermission): AccessTokenPermission {
        permissions.add(id.value to permission)
        return permission
    }

    override fun deletePermission(id: AccessTokenIdentifier, permission: AccessTokenPermission) {
        permissions.removeIf { (tokenId, associatedPermission) -> id.value == tokenId && permission == associatedPermission }
    }

    override fun findAccessTokenPermissionsById(id: AccessTokenIdentifier): Set<AccessTokenPermission> =
        permissions
            .filter { (tokenId) -> tokenId == id.value }
            .map { it.second }
            .toSet()

    override fun addRoute(id: AccessTokenIdentifier, route: Route): Route {
        routes.add(Pair(id.value, route))
        return route
    }

    override fun deleteRoute(id: AccessTokenIdentifier, route: Route) {
        routes.removeIf { it.first == id.value && it.second == route }
    }

    override fun deleteRoutesByPath(id: AccessTokenIdentifier, path: String) {
        routes.removeIf { it.first == id.value && it.second.path == path }
    }

    override fun findAccessTokenRoutesById(id: AccessTokenIdentifier): Set<Route> =
        routes
            .filter { it.first == id.value }
            .map { it.second }
            .toSet()

    override fun findAll(): Collection<AccessToken> {
        val expired = tokens.values.filter { it.isExpired() }
        expired.forEach { evict(it.identifier) }
        return tokens.values.toList()
    }

    override fun countAccessTokens(): Long {
        val expired = tokens.values.filter { it.isExpired() }
        expired.forEach { evict(it.identifier) }
        return tokens.size.toLong()
    }

    private fun evict(id: AccessTokenIdentifier) {
        tokens.remove(id.value)
        permissions.removeIf { it.first == id.value }
        routes.removeIf { it.first == id.value }
    }

}
