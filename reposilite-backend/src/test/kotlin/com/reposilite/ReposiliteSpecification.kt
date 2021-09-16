package com.reposilite

import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.Route
import com.reposilite.token.api.RoutePermission

internal abstract class ReposiliteSpecification : ReposiliteRunner() {

    val base: String
        get() = "http://localhost:$port"

    fun usePredefinedTemporaryAuth(): Pair<String, String> =
        Pair("manager", "manager-secret")

    suspend fun useAuth(name: String, secret: String, routes: Map<String, RoutePermission> = emptyMap()): Pair<String, String> {
        val accessTokenFacade = reposilite.accessTokenFacade
        var accessToken = accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name, secret)).accessToken

        routes.forEach { (route, permission) ->
            accessToken = accessTokenFacade.updateToken(accessToken.withRoute(Route(route, setOf(permission))))
        }

        return Pair(name, secret)
    }

}