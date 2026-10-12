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


package com.reposilite.auth.specification

import com.github.benmanes.caffeine.cache.Ticker
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.reposilite.auth.api.OAuth2SessionResponse
import com.reposilite.auth.application.OAuth2Settings
import com.reposilite.auth.oauth2.OAuth2CodeExchangeRequest
import com.reposilite.auth.oauth2.OAuth2ProviderClient
import com.reposilite.auth.oauth2.OAuth2ProviderMetadata
import com.reposilite.shared.ErrorResponse
import com.reposilite.token.AccessTokenType.TEMPORARY
import com.reposilite.shared.toErrorResult
import io.javalin.http.HttpStatus.BAD_GATEWAY
import org.junit.jupiter.api.BeforeEach
import panda.std.Result
import panda.std.asSuccess
import java.net.URI
import java.net.URLDecoder
import java.time.Duration
import java.time.Instant
import java.util.Date

internal class FakeTicker : Ticker {

    private var nanos = System.nanoTime()

    override fun read(): Long =
        nanos

    fun advance(duration: Duration) {
        nanos += duration.toNanos()
    }

}

internal class FakeOAuth2ProviderClient : OAuth2ProviderClient {

    companion object {
        const val ISSUER = "https://sso.example.com/realms/test"
    }

    var keys = JWKSet()
    var keysRequests = 0
    val idTokens = mutableMapOf<String, String>()
    val exchanges = mutableListOf<OAuth2CodeExchangeRequest>()

    override fun fetchMetadata(issuer: String): Result<OAuth2ProviderMetadata, ErrorResponse> =
        OAuth2ProviderMetadata(
            issuer = issuer,
            authorizationEndpoint = "$issuer/authorize",
            tokenEndpoint = "$issuer/token",
            jwksUri = "$issuer/keys"
        ).asSuccess()

    override fun fetchKeys(jwksUri: String): Result<JWKSet, ErrorResponse> =
        keys.toPublicJWKSet()
            .also { keysRequests++ }
            .asSuccess()

    override fun exchangeCode(request: OAuth2CodeExchangeRequest): Result<String, ErrorResponse> =
        idTokens[request.code]
            ?.also { exchanges.add(request) }
            ?.asSuccess()
            ?: BAD_GATEWAY.toErrorResult("Unknown code")

}

internal abstract class OAuth2Specification : AuthenticationSpecification() {

    protected companion object {
        const val ISSUER = FakeOAuth2ProviderClient.ISSUER
        const val CLIENT_ID = "reposilite"
        const val AUDIENCE = "maven"
        const val BASE_URL = "https://repo.example.com"

        val providerKey: RSAKey = RSAKeyGenerator(2048).keyID("provider-key").generate()
        val foreignKey: RSAKey = RSAKeyGenerator(2048).keyID("provider-key").generate()
    }

    @BeforeEach
    fun configureOAuth2() {
        oauth2Provider.keys = JWKSet(providerKey)
        useOAuth2Settings()
    }

    protected fun useOAuth2Settings(transform: (OAuth2Settings) -> OAuth2Settings = { it }) {
        oauth2Settings.update(
            transform(
                OAuth2Settings(
                    enabled = true,
                    issuer = ISSUER,
                    clientId = CLIENT_ID,
                    clientSecret = "client-secret",
                    userType = TEMPORARY, // both repositories of this specification are in-memory ones that issue temporary identifiers
                    bearerEnabled = true,
                    bearerAudience = AUDIENCE
                )
            )
        )
    }

    protected fun createJwt(
        username: String? = "panda",
        issuer: String = ISSUER,
        audience: String = AUDIENCE,
        expiresAt: Instant = Instant.now().plusSeconds(300),
        nonce: String? = null,
        key: RSAKey = providerKey
    ): String {
        val claims = JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("subject-of-$username")
            .audience(audience)
            .expirationTime(Date.from(expiresAt))
            .also { if (username != null) it.claim("preferred_username", username) }
            .also { if (nonce != null) it.claim("nonce", nonce) }
            .build()

        return SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build(), claims)
            .also { it.sign(RSASSASigner(key)) }
            .serialize()
    }

    protected fun queryOf(url: String): Map<String, String> =
        URI.create(url).rawQuery
            .split("&")
            .associate { parameter -> parameter.split("=", limit = 2).let { (key, value) -> key to URLDecoder.decode(value, Charsets.UTF_8) } }

    /** Simulates identity provider that authorized sign in request and returns code received by the callback */
    protected fun useAuthorizedLogin(username: String, nonce: (String) -> String = { it }): Pair<String, String> {
        val query = queryOf(oauth2Service.startLogin(BASE_URL).get())
        val code = "code-of-$username"
        oauth2Provider.idTokens[code] = createJwt(username = username, audience = CLIENT_ID, nonce = nonce(query.getValue("nonce")))
        return query.getValue("state") to code
    }

    protected fun useSession(username: String): OAuth2SessionResponse {
        val (state, code) = useAuthorizedLogin(username)
        return oauth2Service.redeemCode(oauth2Service.completeLogin(state, code).get()).get()
    }

}
