package com.reposilite.token

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AccessTokenFacadeTest : AccessTokenSpec() {

    @Test
    fun `should create token`() {
        // given: a name and associated secret to register
        val name = "reposilite"

        // when: token is created with the given name
        val accessTokenDetails = createToken(name)

        // then: valid token should be created
        val accessToken = accessTokenDetails.accessToken
        assertEquals(name, accessToken.name)
        assertTrue(LocalDate.now().isEqual(accessToken.createdAt))
    }

    @Test
    fun `should update token` () {
        // given: an existing token and its updated version
        val token = createToken("nanomaven").accessToken
        val updatedToken = token.copy(name = "reposilite")

        // when: token is updated
        accessTokenFacade.updateToken(updatedToken)

        // then: stored token should be updated
        val storedToken = accessTokenFacade.getToken("reposilite")!!
        assertEquals("reposilite", storedToken.name)
    }

    @Test
    fun `should delete token`() {
        // given: an existing token
        val token = createToken("reposilite").accessToken
        val name = token.name

        // when: token is deleted
        val deletedToken = accessTokenFacade.deleteToken(name)

        // then: proper token has been deleted, and it is no longer available
        assertEquals(token, deletedToken)
        assertNull(accessTokenFacade.getToken(name))
    }

    @Test
    fun `should find token by given name`() {
        // given: an existing token
        val token = createToken("reposilite").accessToken

        // when: token is requested by its name
        val foundToken = accessTokenFacade.getToken(token.name)

        // then: proper token has been found
        assertEquals(token, foundToken)
    }

}