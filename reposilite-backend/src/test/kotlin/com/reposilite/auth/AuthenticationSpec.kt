package com.reposilite.auth

import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.CreateAccessTokenResponse
import com.reposilite.token.infrastructure.InMemoryAccessTokenRepository
import net.dzikoysk.dynamiclogger.backend.InMemoryLogger

internal abstract class AuthenticationSpec {

    private val logger = InMemoryLogger()

    private val accessTokenFacade = AccessTokenFacade(
        logger,
        InMemoryAccessTokenRepository()
    )

    protected val authenticationFacade = AuthenticationFacade(
        logger,
        Authenticator(accessTokenFacade),
        SessionService()
    )

    protected fun createToken(alias: String): CreateAccessTokenResponse =
        accessTokenFacade.createAccessToken(alias)

    protected fun createToken(alias: String, secret: String): AccessToken =
        accessTokenFacade.createAccessToken(alias, secret).accessToken

}