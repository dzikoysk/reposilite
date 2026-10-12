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


package com.reposilite.auth.oauth2

import com.nimbusds.jwt.JWTClaimsSet
import com.reposilite.auth.api.OAuth2SessionResponse
import com.reposilite.auth.application.OAuth2Settings
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.unauthorizedError
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenRequest
import org.apache.commons.codec.digest.DigestUtils
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference
import java.time.Duration
import java.util.Base64

internal class OAuth2Service(
    private val journalist: Journalist,
    private val settings: Reference<OAuth2Settings>,
    private val providerClient: OAuth2ProviderClient,
    private val tokenVerifier: OAuth2TokenVerifier,
    private val sessionStore: OAuth2SessionStore,
    private val accessTokenFacade: AccessTokenFacade
) : Journalist {

    internal companion object {
        const val CALLBACK_PATH = "/api/auth/oauth2/callback"
        private const val MAX_USERNAME_LENGTH = 254
    }

    fun isEnabled(): Boolean =
        settings.map { it.enabled }

    fun isBearerEnabled(): Boolean =
        settings.map { it.enabled && it.bearerEnabled }

    /**
     * Starts authorization code flow (with PKCE) and returns URL of the identity provider the user should be redirected to.
     * @param baseUrl public URL of this instance without trailing slash
     */
    fun startLogin(baseUrl: String): Result<String, ErrorResponse> =
        tokenVerifier.metadata().map { metadata ->
            val state = sessionStore.generateSecret()
            val pendingLogin = OAuth2PendingLogin(
                codeVerifier = sessionStore.generateSecret(),
                nonce = sessionStore.generateSecret(),
                redirectUri = baseUrl + CALLBACK_PATH
            )
            sessionStore.addPendingLogin(state, pendingLogin)

            val query = with(settings.get()) {
                linkedMapOf(
                    "response_type" to "code",
                    "client_id" to clientId,
                    "redirect_uri" to pendingLogin.redirectUri,
                    "scope" to scopes,
                    "state" to state,
                    "nonce" to pendingLogin.nonce,
                    "code_challenge" to DigestUtils.sha256(pendingLogin.codeVerifier).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) },
                    "code_challenge_method" to "S256"
                )
            }

            metadata.authorizationEndpoint +
                (if (metadata.authorizationEndpoint.contains('?')) "&" else "?") +
                query.entries.joinToString("&") { (key, value) -> "$key=${value.urlEncoded()}" }
        }

    /**
     * Finishes authorization code flow and returns one-time code that can be exchanged for session credentials with [redeemCode]
     */
    fun completeLogin(state: String?, code: String?): Result<String, ErrorResponse> {
        val pendingLogin = state?.let { sessionStore.takePendingLogin(it) }
            ?: return unauthorizedError("Unknown or expired sign in request")

        if (code.isNullOrBlank()) {
            return unauthorizedError("Identity provider did not authorize the sign in request")
        }

        val (clientId, clientSecret, sessionDuration) = settings.map { Triple(it.clientId, it.clientSecret, it.sessionDurationMinutes) }

        return tokenVerifier.metadata()
            .flatMap { metadata ->
                providerClient.exchangeCode(
                    OAuth2CodeExchangeRequest(
                        tokenEndpoint = metadata.tokenEndpoint,
                        clientId = clientId,
                        clientSecret = clientSecret,
                        code = code,
                        redirectUri = pendingLogin.redirectUri,
                        codeVerifier = pendingLogin.codeVerifier
                    )
                )
            }
            .flatMap { idToken -> tokenVerifier.verify(idToken, audience = clientId, nonce = pendingLogin.nonce) }
            .flatMap { claims -> resolveAccessToken(claims) }
            .map { accessToken ->
                sessionStore.createSession(
                    OAuth2Session(
                        name = accessToken.name,
                        identifier = accessToken.identifier,
                        duration = Duration.ofMinutes(sessionDuration.coerceAtLeast(1))
                    )
                )
            }
            .onError { logger.debug("OAuth2 | Sign in rejected due to $it") }
    }

    fun redeemCode(code: String): Result<OAuth2SessionResponse, ErrorResponse> =
        sessionStore.takeGrant(code)
            ?.asSuccess()
            ?: unauthorizedError("Unknown or expired sign in code")

    fun authenticateSession(name: String, secret: String): Result<AccessTokenDto, ErrorResponse> =
        sessionStore.findSession(secret)
            ?.takeIf { it.name == name }
            ?.let { accessTokenFacade.getAccessTokenById(it.identifier) }
            ?.takeIf { it.name == name && !it.isExpired() }
            ?.asSuccess()
            ?: unauthorizedError("Invalid session credentials")

    fun logout(secret: String) =
        sessionStore.deleteSession(secret)

    fun authenticateBearer(token: String): Result<AccessTokenDto, ErrorResponse> {
        val audience = settings.map { it.bearerAudience }

        return when {
            audience.isBlank() -> unauthorizedError("Bearer audience is not configured")
            token.count { it == '.' } != 2 -> unauthorizedError("Secret is not a signed JWT")
            else -> tokenVerifier.verify(token, audience).flatMap { resolveAccessToken(it) }
        }
    }

    @Synchronized
    private fun resolveAccessToken(claims: JWTClaimsSet): Result<AccessTokenDto, ErrorResponse> {
        val (usernameClaim, userType) = settings.map { it.usernameClaim to it.userType }

        val username = claims.getClaim(usernameClaim) as? String
            ?: return unauthorizedError("Token does not contain '$usernameClaim' claim")

        if (username.isBlank() || username.contains(':') || username.length > MAX_USERNAME_LENGTH) {
            return unauthorizedError("Username provided by '$usernameClaim' claim cannot be used as access token name")
        }

        val accessToken = accessTokenFacade.getAccessToken(username)
            ?: accessTokenFacade.createAccessToken(CreateAccessTokenRequest(type = userType, name = username))
                .accessToken
                .also { logger.info("OAuth2 | Created ${userType.name.lowercase()} access token for $username") }

        return accessToken
            .takeIf { !it.isExpired() }
            ?.asSuccess()
            ?: unauthorizedError("Invalid authorization credentials")
    }

    override fun getLogger(): Logger =
        journalist.logger

}
