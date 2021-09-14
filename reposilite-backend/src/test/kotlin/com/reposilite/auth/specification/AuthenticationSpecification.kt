package com.reposilite.auth.specification

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.token.AccessTokenSpec

internal abstract class AuthenticationSpecification : AccessTokenSpec() {

    private val logger = InMemoryLogger()

    protected val authenticationFacade = AuthenticationFacade(
        logger,
        accessTokenFacade
    )

}