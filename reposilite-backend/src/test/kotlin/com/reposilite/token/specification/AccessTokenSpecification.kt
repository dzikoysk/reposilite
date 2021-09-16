package com.reposilite.token.specification

import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenResponse
import com.reposilite.token.infrastructure.InMemoryAccessTokenRepository

internal open class AccessTokenSpecification {

    protected val accessTokenFacade = AccessTokenFacade(InMemoryAccessTokenRepository(), InMemoryAccessTokenRepository())

    protected suspend fun createToken(name: String): CreateAccessTokenResponse =
        accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name))

    protected suspend fun createToken(name: String, secret: String): AccessToken =
        accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name, secret)).accessToken

}