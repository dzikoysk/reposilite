package com.reposilite.auth

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.token.AccessTokenSpec

internal abstract class AuthenticationSpec : AccessTokenSpec() {

    private val logger = InMemoryLogger()

    protected val authenticationFacade = AuthenticationFacade(
        logger,
        accessTokenFacade
    )

}