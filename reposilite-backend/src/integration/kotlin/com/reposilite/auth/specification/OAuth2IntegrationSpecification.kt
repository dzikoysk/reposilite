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

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.reposilite.ReposiliteSpecification
import com.reposilite.auth.application.AuthenticationSettings
import com.reposilite.auth.application.OAuth2Settings
import com.reposilite.configuration.shared.SharedConfigurationFacade
import io.javalin.Javalin
import io.javalin.http.Header
import io.javalin.http.HttpStatus.UNAUTHORIZED
import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.AfterEach
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal abstract class OAuth2IntegrationSpecification : ReposiliteSpecification() {

    protected companion object {
        const val CLIENT_ID = "reposilite-client"
        const val CLIENT_SECRET = "reposilite-client-secret"
        const val AUDIENCE = "maven"

        val providerKey: RSAKey = RSAKeyGenerator(2048).keyID("provider-key").generate()
    }

    private class AuthorizationRequest(val nonce: String, val codeChallenge: String, val redirectUri: String, val username: String)

    private val authorizationRequests = ConcurrentHashMap<String, AuthorizationRequest>()
    private var identityProvider: Javalin? = null
    private val browser = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()

    /** User that is currently signed in to the identity provider */
    protected var providerUser = "panda"

    protected val issuer: String
        get() = "http://localhost:${identityProvider!!.port()}"

    override fun overrideSharedConfiguration(sharedConfigurationFacade: SharedConfigurationFacade) {
        if (identityProvider == null) {
            identityProvider = startIdentityProvider()
        }

        sharedConfigurationFacade.getDomainSettings<AuthenticationSettings>().update {
            it.copy(
                oauth2 = OAuth2Settings(
                    enabled = true,
                    issuer = issuer,
                    clientId = CLIENT_ID,
                    clientSecret = CLIENT_SECRET,
                    bearerEnabled = true,
                    bearerAudience = AUDIENCE
                )
            )
        }
    }

    @AfterEach
    fun stopIdentityProvider() {
        identityProvider?.stop()
    }

    private fun startIdentityProvider(): Javalin =
        Javalin.create { config ->
            config.routes
                .get("/.well-known/openid-configuration") {
                    it.json(
                        mapOf(
                            "issuer" to issuer,
                            "authorization_endpoint" to "$issuer/authorize",
                            "token_endpoint" to "$issuer/token",
                            "jwks_uri" to "$issuer/keys"
                        )
                    )
                }
                .get("/keys") {
                    it.contentType("application/json").result(JWKSet(providerKey).toString(true))
                }
                .get("/authorize") {
                    val code = UUID.randomUUID().toString()
                    val redirectUri = it.queryParam("redirect_uri")!!

                    authorizationRequests[code] = AuthorizationRequest(
                        nonce = it.queryParam("nonce")!!,
                        codeChallenge = it.queryParam("code_challenge")!!,
                        redirectUri = redirectUri,
                        username = providerUser
                    )

                    it.redirect("$redirectUri?code=$code&state=${URLEncoder.encode(it.queryParam("state")!!, Charsets.UTF_8)}")
                }
                .post("/token") {
                    val request = authorizationRequests.remove(it.formParam("code"))
                    val expectedAuthorization = "Basic " + Base64.getEncoder().encodeToString("$CLIENT_ID:$CLIENT_SECRET".toByteArray())
                    val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(DigestUtils.sha256(it.formParam("code_verifier") ?: ""))

                    when {
                        request == null
                            || it.header(Header.AUTHORIZATION) != expectedAuthorization
                            || it.formParam("grant_type") != "authorization_code"
                            || it.formParam("redirect_uri") != request.redirectUri
                            || challenge != request.codeChallenge -> it.status(UNAUTHORIZED).json(mapOf("error" to "invalid_grant"))
                        else -> it.json(
                            mapOf(
                                "token_type" to "Bearer",
                                "access_token" to "opaque-access-token",
                                "id_token" to createJwt(username = request.username, audience = CLIENT_ID, nonce = request.nonce)
                            )
                        )
                    }
                }
        }.start(0)

    protected fun createJwt(
        username: String = providerUser,
        audience: String = AUDIENCE,
        expiresAt: Instant = Instant.now().plusSeconds(300),
        nonce: String? = null
    ): String {
        val claims = JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(username)
            .audience(audience)
            .expirationTime(Date.from(expiresAt))
            .claim("preferred_username", username)
            .also { if (nonce != null) it.claim("nonce", nonce) }
            .build()

        return SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(providerKey.keyID).build(), claims)
            .also { it.sign(RSASSASigner(providerKey)) }
            .serialize()
    }

    protected fun useOAuth2Settings(transform: (OAuth2Settings) -> OAuth2Settings) {
        useFacade<SharedConfigurationFacade>()
            .getDomainSettings<AuthenticationSettings>()
            .update { it.copy(oauth2 = transform(it.oauth2)) }
    }

    /** Sends request like a browser that does not follow redirects on its own */
    protected fun browse(uri: String): HttpResponse<String> =
        browser.send(HttpRequest.newBuilder(URI.create(uri)).GET().build(), BodyHandlers.ofString())

    protected fun HttpResponse<*>.location(): String =
        headers().firstValue("Location").orElseThrow()

    protected fun queryOf(url: String): Map<String, String> =
        URI.create(url).rawQuery
            .split("&")
            .associate { parameter -> parameter.split("=", limit = 2).let { (key, value) -> key to URLDecoder.decode(value, Charsets.UTF_8) } }

}
