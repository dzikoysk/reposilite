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

package com.reposilite

import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.Route
import com.reposilite.token.api.RoutePermission

internal abstract class ReposiliteSpecification : ReposiliteRunner() {

    val base: String
        get() = "http://localhost:$port"

    fun usePredefinedTemporaryAuth(): Pair<String, String> =
        Pair("manager", "manager-secret")

    fun useAuth(name: String, secret: String, routes: Map<String, RoutePermission> = emptyMap()): Pair<String, String> {
        val accessTokenFacade = reposilite.accessTokenFacade
        var accessToken = accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name, secret)).accessToken

        routes.forEach { (route, permission) ->
            accessToken = accessTokenFacade.updateToken(accessToken.withRoute(Route(route, setOf(permission))))
        }

        return Pair(name, secret)
    }

}