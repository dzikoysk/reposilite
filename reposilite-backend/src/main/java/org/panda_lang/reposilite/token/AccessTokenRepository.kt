package org.panda_lang.reposilite.token

import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.api.Route

internal interface AccessTokenRepository {

    fun createAccessToken(accessToken: AccessToken)

    fun createRoute(accessToken: AccessToken, route: Route)

    fun deleteAccessTokenByAlias(alias: String)

    fun deleteRoute(route: Route)

    fun findAccessTokenByAlias(alias: String): AccessToken?

    fun findAll(): Collection<AccessToken>

    fun countAccessTokens(): Long

}