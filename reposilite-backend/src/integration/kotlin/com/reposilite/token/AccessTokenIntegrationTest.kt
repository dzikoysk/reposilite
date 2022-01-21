@file:Suppress("FunctionName")

package com.reposilite.token

import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenResponse
import com.reposilite.token.api.CreateAccessTokenWithNoNameRequest
import com.reposilite.token.specification.AccessTokenIntegrationSpecification
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.OK
import io.javalin.http.HttpCode.UNAUTHORIZED
import io.javalin.http.HttpCode.FORBIDDEN
import io.javalin.http.HttpCode.BAD_REQUEST
import kong.unirest.Unirest.delete
import kong.unirest.Unirest.get
import kong.unirest.Unirest.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal abstract class AccessTokenIntegrationTest : AccessTokenIntegrationSpecification() {

    @Test
    fun `should list tokens with entitled token`() {
        // given: existing tokens
        val (temporaryName, temporarySecret) = usePredefinedTemporaryAuth()
        val (persistentToken, persistentSecret) = useToken("persistent", "persistent-secret")

        // when: list of tokens is requested without valid access token
        val unauthorizedResponse = get("$base/api/tokens")
            .basicAuth(persistentToken.name, persistentSecret)
            .asJacksonObject(ErrorResponse::class)

        // then: request is rejected
        assertErrorResponse(UNAUTHORIZED, unauthorizedResponse)

        // when: list of tokens is requested with valid token
        val response = get("$base/api/tokens")
            .basicAuth(temporaryName, temporarySecret)
            .asJacksonObject(Array<AccessTokenDto>::class)

        // then: response contains list of all
        assertSuccessResponse(OK, response) { tokens ->
            assertEquals(2, tokens.size)
        }
    }

    @Test
    fun `should allow to check token details to entitled token`() {
        // given: existing tokens
        val (notAllowedToken, notAllowedSecret) = useToken("not-allowed", "secret")
        val (allowedToken, allowedSecret) = useToken("allowed", "secret")

        // when: tokens is requested without valid credentials
        val forbiddenResponse = get("$base/api/tokens/${allowedToken.name}")
            .basicAuth(notAllowedToken.name, notAllowedSecret)
            .asJacksonObject(ErrorResponse::class)

        // then: request is rejected by server
        assertErrorResponse(FORBIDDEN, forbiddenResponse)

        // when: token is requested with valid access token
        val response = get("$base/api/tokens/${allowedToken.name}")
            .basicAuth(allowedToken.name, allowedSecret)
            .asJacksonObject(AccessTokenDto::class)

        // then: request is rejected by server
        assertSuccessResponse(OK, response) {
            assertEquals(allowedToken.name, it.name)
        }
    }

    @Test
    fun `should generate a new token with entitled token`() {
        // given: existing tokens to use and details about token to create
        val (managerName, managerSecret) = usePredefinedTemporaryAuth()
        val (notAllowedToken, notAllowedSecret) = useToken("not-allowed", "secret")
        val (name, secret, permissions) = useTokenDescription("name", "secret", setOf(MANAGER))

        // when: not entitled token attempts to generate a new token
        val badRequest = put("$base/api/tokens/$name")
            .basicAuth(notAllowedToken.name, notAllowedSecret)
            .body(CreateAccessTokenWithNoNameRequest(PERSISTENT, secret, permissions.map { it.shortcut }.toSet()))
            .asJacksonObject(ErrorResponse::class)

        // then: the unauthorized request is rejected
        assertErrorResponse(BAD_REQUEST, badRequest)

        // when: valid manager token creates a new token
        val response = put("$base/api/tokens/$name")
            .basicAuth(managerName, managerSecret)
            .body(CreateAccessTokenWithNoNameRequest(PERSISTENT, secret, permissions.map { it.shortcut }.toSet()))
            .asJacksonObject(CreateAccessTokenResponse::class)

        // then: response contains valid token with generated secret
        assertSuccessResponse(OK, response) {
            assertEquals(name, it.accessToken.name)
            assertEquals(secret, it.secret)
            assertEquals(permissions, getPermissions(it.accessToken.identifier))
        }
    }

    @Test
    fun `should delete given token with entitled token`() {
        val (managerName, managerSecret) = usePredefinedTemporaryAuth()
        val (notAllowedToken, notAllowedSecret) = useToken("not-allowed", "secret")
        val (tokenToDelete) = useToken("token-to-delete", "secret")

        // when: not entitled token attempts to generate a new token
        val unauthorized = delete("$base/api/tokens/${tokenToDelete.name}")
            .basicAuth(notAllowedToken.name, notAllowedSecret)
            .asJacksonObject(ErrorResponse::class)

        // then: the unauthorized request is rejected
        assertErrorResponse(UNAUTHORIZED, unauthorized)

        // when: valid manager token creates a new token
        val response = delete("$base/api/tokens/${tokenToDelete.name}")
            .basicAuth(managerName, managerSecret)
            .asEmpty()

        // then: the given token should be deleted
        assertSuccessResponse(OK, response)
    }

}