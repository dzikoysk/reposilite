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

package org.panda_lang.reposilite.token.infrastructure

import org.panda_lang.reposilite.token.AccessTokenRepository
import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.api.Route
import java.util.concurrent.ConcurrentHashMap

internal class InMemoryAccessTokenRepository : AccessTokenRepository {

    private val tokens: MutableMap<Int, AccessToken> = ConcurrentHashMap()
    private val routes: MutableMap<Int, Route> = ConcurrentHashMap()

    override fun createAccessToken(accessToken: AccessToken) {
        tokens[accessToken.id] = accessToken
    }

    override fun createRoute(accessToken: AccessToken, route: Route) {
        routes[route.id] = route
    }

    override fun updateAccessToken(accessToken: AccessToken) {
        tokens[accessToken.id] = accessToken
    }

    override fun deleteAccessTokenByAlias(alias: String) {
        tokens.filter { it.value.alias == alias }.forEach { tokens.remove(it.key) }
    }

    override fun deleteRoute(route: Route) {
        routes.filter { it.value.id == route.id }.forEach { routes.remove(it.key) }
    }

    override fun findAccessTokenByAlias(alias: String): AccessToken? =
        tokens.values.firstOrNull { it.alias == alias }

    override fun findAll(): Collection<AccessToken> =
        tokens.values

    override fun countAccessTokens(): Long =
        tokens.size.toLong()

}