package com.reposilite.token

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AccessTokenFacadeTest : AccessTokenSpec() {

    @Test
    fun `should create token`() {
        // given: an alias and associated secret to register
        val alias = "reposilite"

        // when: token is created with the given alias
        val accessTokenDetails = accessTokenFacade.createAccessToken(alias)

        // then: valid token should be created
        val accessToken = accessTokenDetails.accessToken
        assertEquals(alias, accessToken.alias)
        assertTrue(LocalDate.now().isEqual(accessToken.createdAt))
    }

    @Test
    fun `should update token` () {
        // given: an existing token and its updated version
        val token = accessTokenFacade.createAccessToken("nanomaven").accessToken
        val updatedToken = token.copy(alias = "reposilite")

        // when: token is updated
        accessTokenFacade.updateToken(updatedToken)

        // then: stored token should be updated
        val storedToken = accessTokenFacade.getToken("reposilite")!!
        assertEquals("reposilite", storedToken.alias)
    }

    @Test
    fun `should delete token`() {
        // given: an existing token
        val token = accessTokenFacade.createAccessToken("reposilite").accessToken
        val alias = token.alias

        // when: token is deleted
        val deletedToken = accessTokenFacade.deleteToken(alias)

        // then: proper token has been deleted, and it is no longer available
        assertEquals(token, deletedToken)
        assertNull(accessTokenFacade.getToken(alias))
    }

    @Test
    fun `should find token by given alias`() {
        // given: an existing token
        val token = accessTokenFacade.createAccessToken("reposilite").accessToken

        // when: token is requested by its name
        val foundToken = accessTokenFacade.getToken(token.alias)

        // then: proper token has been found
        assertEquals(token, foundToken)
    }

}