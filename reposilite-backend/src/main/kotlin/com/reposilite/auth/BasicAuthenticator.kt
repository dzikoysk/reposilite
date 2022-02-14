package com.reposilite.auth

import com.reposilite.auth.api.AuthenticationRequest
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.UNAUTHORIZED
import panda.std.Result
import panda.std.asSuccess

internal class BasicAuthenticator(private val accessTokenFacade: AccessTokenFacade) : Authenticator {

    override fun authenticate(authenticationRequest: AuthenticationRequest): Result<AccessTokenDto, ErrorResponse> =
        accessTokenFacade.getAccessToken(authenticationRequest.name)
            ?.takeIf { accessTokenFacade.secretMatches(it.identifier, authenticationRequest.secret) }
            ?.asSuccess()
            ?: errorResponse(UNAUTHORIZED, "Invalid authorization credentials")

    override fun name(): String =
        "Basic"

}