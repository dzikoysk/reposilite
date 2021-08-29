package com.reposilite.auth

import com.reposilite.token.AccessTokenSpec
import net.dzikoysk.dynamiclogger.backend.InMemoryLogger

internal abstract class AuthenticationSpec : AccessTokenSpec() {

    private val logger = InMemoryLogger()

    protected val authenticationFacade = AuthenticationFacade(
        logger,
        accessTokenFacade,
        SessionService()
    )

}