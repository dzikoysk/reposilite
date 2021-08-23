package com.reposilite.auth

import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.UNAUTHORIZED
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class AuthenticationFacadeTest : AuthenticationSpec() {

    @Test
    fun `should reject authentication request with invalid credentials`() {
        // given: an existing token with unknown secret
        val name = "name"
        createToken(name)

        // when: an authentication is requested with invalid credentials
        val response = authenticationFacade.authenticateByCredentials("$name:invalid-secret")

        // then: the request has been rejected
        assertError(ErrorResponse(UNAUTHORIZED, "Invalid authorization credentials"), response)
    }

    @Test
    fun `should authenticate by valid credentials`() {
        // given: a credentials to the existing token
        val name = "name"
        val secret = "secret"
        val accessToken = createToken(name, secret)

        // when: an authentication is requested with valid credentials
        val response = authenticationFacade.authenticateByCredentials("$name:$secret")

        // then: the request has been authorized
        assertOk(accessToken, response)
    }

}