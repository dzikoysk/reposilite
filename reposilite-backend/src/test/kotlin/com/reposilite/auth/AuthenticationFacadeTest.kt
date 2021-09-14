package com.reposilite.auth

import com.reposilite.auth.specification.AuthenticationSpecification
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.UNAUTHORIZED
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class AuthenticationFacadeTest : AuthenticationSpecification() {

    @Test
    fun `should reject authentication request with invalid credentials`() = runBlocking {
        // given: an existing token with unknown secret
        val name = "name"
        createToken(name)

        // when: an authentication is requested with invalid credentials
        val response = authenticationFacade.authenticateByCredentials(name, "invalid-secret")

        // then: the request has been rejected
        assertError(ErrorResponse(UNAUTHORIZED, "Invalid authorization credentials"), response)
    }

    @Test
    fun `should authenticate by valid credentials`() = runBlocking {
        // given: a credentials to the existing token
        val name = "name"
        val secret = "secret"
        val accessToken = createToken(name, secret)

        // when: an authentication is requested with valid credentials
        val response = authenticationFacade.authenticateByCredentials(name, secret)

        // then: the request has been authorized
        assertOk(accessToken, response)
    }

}