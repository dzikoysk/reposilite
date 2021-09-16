package com.reposilite.auth.specification

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.token.specification.AccessTokenSpecification

internal abstract class AuthenticationSpecification : AccessTokenSpecification() {

    private val logger = InMemoryLogger()

    protected val authenticationFacade = AuthenticationFacade(
        logger,
        accessTokenFacade
    )

}