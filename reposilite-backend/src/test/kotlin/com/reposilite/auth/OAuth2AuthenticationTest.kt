/*
 * Copyright (c) 2020-2026 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.reposilite.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import com.nimbusds.jwt.SignedJWT
import com.reposilite.auth.api.Credentials
import com.reposilite.auth.specification.OAuth2Specification
import com.reposilite.token.AccessTokenPermission.MANAGER
import org.apache.commons.codec.digest.DigestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk
import java.time.Duration
import java.time.Instant
import java.util.Base64

internal class OAuth2AuthenticationTest : OAuth2Specification() {

    private fun authenticate(name: String, secret: String) =
        authenticationFacade.authenticateByCredentials(Credentials("host", name, secret))

    @Nested
    inner class BearerTokens {

        @Test
        fun `should create access token without permissions for unknown user`() {
            // given: a valid token of user that has never been seen before
            val jwt = createJwt(username = "panda")

            // when: the token is used as bearer token (anonymous credentials)
            val response = authenticate("", jwt)

            // then: access token named after the user has been created
            val accessToken = assertOk(response)
            assertThat(accessToken.name).isEqualTo("panda")
            assertThat(accessToken).isEqualTo(accessTokenFacade.getAccessToken("panda"))
            assertThat(accessTokenFacade.getPermissions(accessToken.identifier)).isEmpty()
            assertThat(accessTokenFacade.getRoutes(accessToken.identifier)).isEmpty()
        }

        @Test
        fun `should authenticate with token provided as a secret of basic credentials`() {
            // when: the token is used as a password with any username
            val response = authenticate("ci", createJwt(username = "panda"))

            // then: identity comes from the token
            assertThat(assertOk(response).name).isEqualTo("panda")
        }

        @Test
        fun `should reuse existing access token`() {
            // given: an existing access token with permissions
            val existingToken = createToken("panda", "secret")
            accessTokenFacade.addPermission(existingToken.identifier, MANAGER)

            // when: user with the same name authenticates
            val response = authenticate("", createJwt(username = "panda"))

            // then: the existing token is used
            assertOk(existingToken, response)
            assertThat(accessTokenFacade.getAccessTokens()).hasSize(1)
        }

        @Test
        fun `should verify every request`() {
            // given: a token that has been already accepted
            val jwt = createJwt()
            assertOk(authenticate("", jwt))

            // when: bearer tokens are disabled and the same credentials are used again
            useOAuth2Settings { it.copy(bearerEnabled = false) }
            val response = authenticate("", jwt)

            // then: the previous result is not reused
            assertError(response)
        }

        @Test
        fun `should reject expired token`() {
            assertError(authenticate("", createJwt(expiresAt = Instant.now().minusSeconds(600))))
        }

        @Test
        fun `should reject token issued for another audience`() {
            assertError(authenticate("", createJwt(audience = "another-service")))
            assertError(authenticate("", createJwt(audience = CLIENT_ID)))
        }

        @Test
        fun `should reject token of another issuer`() {
            assertError(authenticate("", createJwt(issuer = "https://evil.example.com")))
        }

        @Test
        fun `should reject token signed with a key unknown to the provider`() {
            assertError(authenticate("", createJwt(key = foreignKey)))
        }

        @Test
        fun `should reject unsecured token`() {
            // given: a token with valid claims and 'none' algorithm
            val claims = SignedJWT.parse(createJwt()).jwtClaimsSet
            val jwt = PlainJWT(claims).serialize() + "signature"

            // then: it is rejected
            assertError(authenticate("", jwt))
            assertError(authenticate("", PlainJWT(claims).serialize()))
        }

        @Test
        fun `should reject token signed with symmetric algorithm using public key`() {
            // given: a token signed with HMAC using publicly known key of the provider
            val jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.HS256).keyID(providerKey.keyID).build(), SignedJWT.parse(createJwt()).jwtClaimsSet)
                .also { it.sign(MACSigner(providerKey.toRSAPublicKey().encoded)) }
                .serialize()

            // then: it is rejected
            assertError(authenticate("", jwt))
        }

        @Test
        fun `should reject token without usable username`() {
            assertError(authenticate("", createJwt(username = null)))
            assertError(authenticate("", createJwt(username = "")))
            assertError(authenticate("", createJwt(username = "repo:org/project")))
            assertError(authenticate("", createJwt(username = "a".repeat(255))))
            assertThat(accessTokenFacade.getAccessTokens()).isEmpty()
        }

        @Test
        fun `should use configured username claim`() {
            // given: username provided by subject
            useOAuth2Settings { it.copy(usernameClaim = "sub") }

            // then: access token is named after the subject
            assertThat(assertOk(authenticate("", createJwt(username = "panda"))).name).isEqualTo("subject-of-panda")
        }

        @Test
        fun `should reject tokens if bearer authentication is not enabled`() {
            val jwt = createJwt()

            useOAuth2Settings { it.copy(bearerEnabled = false) }
            assertError(authenticate("", jwt))

            useOAuth2Settings { it.copy(enabled = false) }
            assertError(authenticate("", jwt))

            useOAuth2Settings { it.copy(bearerAudience = "") }
            assertError(authenticate("", jwt))

            assertThat(accessTokenFacade.getAccessTokens()).isEmpty()
        }

        @Test
        fun `should fetch keys again after key rotation`() {
            // given: keys cached by the previous request
            assertOk(authenticate("", createJwt(username = "panda")))
            assertThat(oauth2Provider.keysRequests).isEqualTo(1)
            assertOk(authenticate("", createJwt(username = "panda")))
            assertThat(oauth2Provider.keysRequests).isEqualTo(1)

            // when: provider rotates the key after a while
            oauth2Ticker.advance(Duration.ofMinutes(2))
            val rotatedKey = RSAKeyGenerator(2048).keyID("rotated-key").generate()
            oauth2Provider.keys = JWKSet(rotatedKey)

            // then: token signed with the new key is accepted
            assertOk(authenticate("", createJwt(key = rotatedKey)))
            assertThat(oauth2Provider.keysRequests).isEqualTo(2)

            // and: unknown keys do not cause a request to the provider every time
            val unknownKey = RSAKeyGenerator(2048).keyID("unknown-key").generate()
            repeat(3) { assertError(authenticate("", createJwt(key = unknownKey))) }
            assertThat(oauth2Provider.keysRequests).isEqualTo(2)
        }

        @Test
        fun `should not affect authentication with access tokens`() {
            // given: a regular access token
            val accessToken = createToken("name", "secret")

            // then: it works as usual
            assertOk(accessToken, authenticate("name", "secret"))
            assertError(authenticate("name", "invalid.secret.value"))
        }

    }

    @Nested
    inner class Sessions {

        @Test
        fun `should redirect to identity provider`() {
            // when: sign in is requested
            val url = assertOk(oauth2Service.startLogin(BASE_URL))
            val query = queryOf(url)

            // then: user is redirected to the provider with a request that uses PKCE
            assertThat(url).startsWith("$ISSUER/authorize?")
            assertThat(query).containsEntry("response_type", "code")
            assertThat(query).containsEntry("client_id", CLIENT_ID)
            assertThat(query).containsEntry("redirect_uri", "$BASE_URL/api/auth/oauth2/callback")
            assertThat(query).containsEntry("scope", "openid profile email")
            assertThat(query).containsEntry("code_challenge_method", "S256")
            assertThat(query).containsKeys("state", "nonce", "code_challenge")
        }

        @Test
        fun `should sign in with authorization code`() {
            // given: a sign in request authorized by the provider
            val (state, code) = useAuthorizedLogin("panda")

            // when: provider redirects back and the one-time code is exchanged for session
            val session = assertOk(oauth2Service.redeemCode(assertOk(oauth2Service.completeLogin(state, code))))

            // then: code has been exchanged using the verifier of requested challenge
            val exchange = oauth2Provider.exchanges.single()
            assertThat(exchange.tokenEndpoint).isEqualTo("$ISSUER/token")
            assertThat(exchange.clientId).isEqualTo(CLIENT_ID)
            assertThat(exchange.clientSecret).isEqualTo("client-secret")
            assertThat(exchange.redirectUri).isEqualTo("$BASE_URL/api/auth/oauth2/callback")
            assertThat(exchange.codeVerifier).isNotBlank()

            // and: session credentials authenticate as access token of the user
            assertThat(session.name).isEqualTo("panda")
            val accessToken = assertOk(authenticate(session.name, session.secret))
            assertThat(accessToken).isEqualTo(accessTokenFacade.getAccessToken("panda"))
            assertThat(accessTokenFacade.getPermissions(accessToken.identifier)).isEmpty()

            // and: session secret is not a secret of the access token
            assertThat(accessTokenFacade.secretMatches(accessToken.identifier, session.secret)).isFalse()
        }

        @Test
        fun `should send challenge that matches the verifier`() {
            // given: a challenge sent to the provider
            val query = queryOf(oauth2Service.startLogin(BASE_URL).get())
            oauth2Provider.idTokens["code"] = createJwt(audience = CLIENT_ID, nonce = query.getValue("nonce"))

            // when: the code is exchanged
            assertOk(oauth2Service.completeLogin(query.getValue("state"), "code"))

            // then: verifier matches the challenge
            val verifier = oauth2Provider.exchanges.single().codeVerifier
            assertThat(Base64.getUrlEncoder().withoutPadding().encodeToString(DigestUtils.sha256(verifier))).isEqualTo(query.getValue("code_challenge"))
        }

        @Test
        fun `should reject unknown and reused state`() {
            // given: an authorized sign in request
            val (state, code) = useAuthorizedLogin("panda")

            // then: only the first callback with known state is accepted
            assertError(oauth2Service.completeLogin("unknown-state", code))
            assertError(oauth2Service.completeLogin(null, code))
            assertOk(oauth2Service.completeLogin(state, code))
            assertError(oauth2Service.completeLogin(state, code))
        }

        @Test
        fun `should reject expired sign in request`() {
            // given: a sign in request authorized after too long time
            val (state, code) = useAuthorizedLogin("panda")
            oauth2Ticker.advance(Duration.ofMinutes(6))

            // then: callback is rejected
            assertError(oauth2Service.completeLogin(state, code))
        }

        @Test
        fun `should reject callback without code`() {
            val (state) = useAuthorizedLogin("panda")
            assertError(oauth2Service.completeLogin(state, null))
        }

        @Test
        fun `should reject id token issued for another sign in request`() {
            // given: an id token with nonce of another request
            val (state, code) = useAuthorizedLogin("panda", nonce = { "another-nonce" })

            // then: sign in is rejected
            assertError(oauth2Service.completeLogin(state, code))
            assertThat(accessTokenFacade.getAccessToken("panda")).isNull()
        }

        @Test
        fun `should reject id token issued for another client`() {
            // given: an id token issued for another audience
            val query = queryOf(oauth2Service.startLogin(BASE_URL).get())
            oauth2Provider.idTokens["code"] = createJwt(audience = "another-client", nonce = query.getValue("nonce"))

            // then: sign in is rejected
            assertError(oauth2Service.completeLogin(query.getValue("state"), "code"))
        }

        @Test
        fun `should exchange one-time code only once`() {
            // given: a one-time code
            val (state, code) = useAuthorizedLogin("panda")
            val oneTimeCode = assertOk(oauth2Service.completeLogin(state, code))

            // then: only the first exchange succeeds
            assertOk(oauth2Service.redeemCode(oneTimeCode))
            assertError(oauth2Service.redeemCode(oneTimeCode))
            assertError(oauth2Service.redeemCode("unknown"))
        }

        @Test
        fun `should expire one-time code`() {
            // given: a one-time code that has not been used in time
            val (state, code) = useAuthorizedLogin("panda")
            val oneTimeCode = assertOk(oauth2Service.completeLogin(state, code))
            oauth2Ticker.advance(Duration.ofMinutes(2))

            // then: it cannot be exchanged
            assertError(oauth2Service.redeemCode(oneTimeCode))
        }

        @Test
        fun `should expire session`() {
            // given: a session that lasts 10 minutes
            useOAuth2Settings { it.copy(sessionDurationMinutes = 10) }
            val session = useSession("panda")

            // when: 9 minutes passed
            oauth2Ticker.advance(Duration.ofMinutes(9))
            // then: session is still valid
            assertOk(authenticate(session.name, session.secret))

            // when: session duration has been exceeded
            oauth2Ticker.advance(Duration.ofMinutes(2))
            // then: session is rejected
            assertError(authenticate(session.name, session.secret))
        }

        @Test
        fun `should close session`() {
            // given: two sessions of the same user
            val session = useSession("panda")
            val otherSession = useSession("panda")
            assertOk(authenticate(session.name, session.secret))

            // when: one of them is closed
            oauth2Service.logout(session.secret)

            // then: only the other one remains valid
            assertError(authenticate(session.name, session.secret))
            assertOk(authenticate(otherSession.name, otherSession.secret))
        }

        @Test
        fun `should reject session secret used with another name`() {
            // given: a session and another existing token
            val session = useSession("panda")
            createToken("manager", "secret")

            // then: session secret cannot be used to access the other token
            assertError(authenticate("manager", session.secret))
            assertError(authenticate("", session.secret))
        }

        @Test
        fun `should reject session of deleted access token`() {
            // given: a session of user whose token has been deleted
            val session = useSession("panda")
            accessTokenFacade.deleteToken(accessTokenFacade.getAccessToken("panda")!!.identifier)

            // then: session is rejected
            assertError(authenticate(session.name, session.secret))
        }

        @Test
        fun `should reject sessions if oauth2 is disabled`() {
            // given: a session created before the feature has been disabled
            val session = useSession("panda")
            useOAuth2Settings { it.copy(enabled = false) }

            // then: session is rejected
            assertError(authenticate(session.name, session.secret))
        }

    }

}
