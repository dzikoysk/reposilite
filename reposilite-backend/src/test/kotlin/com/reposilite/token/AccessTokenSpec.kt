package com.reposilite.token

import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenResponse
import com.reposilite.token.infrastructure.InMemoryAccessTokenRepository

internal open class AccessTokenSpec {

    protected val accessTokenFacade = AccessTokenFacade(InMemoryAccessTokenRepository(), InMemoryAccessTokenRepository())

    protected fun createToken(name: String): CreateAccessTokenResponse =
        accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name))

    protected fun createToken(name: String, secret: String): AccessToken =
        accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name, secret)).accessToken

}