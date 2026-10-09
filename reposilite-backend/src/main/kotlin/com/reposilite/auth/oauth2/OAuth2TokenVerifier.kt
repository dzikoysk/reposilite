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

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.KeySourceException
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.reposilite.auth.application.OAuth2Settings
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.unauthorizedError
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference
import java.time.Duration

/**
 * Verifies JWTs against the keys published by the configured OpenID Connect provider.
 * Provider's metadata and keys are cached, the keys are fetched again once a token is signed by an unknown one.
 */
internal class OAuth2TokenVerifier(
    private val providerClient: OAuth2ProviderClient,
    private val settings: Reference<OAuth2Settings>,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    private companion object {
        private val METADATA_LIFETIME = Duration.ofHours(1).toMillis()
        private val KEYS_LIFETIME = Duration.ofHours(1).toMillis()
        private val KEYS_REFRESH_COOLDOWN = Duration.ofMinutes(1).toMillis()

        // Only asymmetric signatures, so the token cannot be forged with publicly known material
        private val SUPPORTED_ALGORITHMS: Set<JWSAlgorithm> = JWSAlgorithm.Family.RSA + JWSAlgorithm.Family.EC
        private val SUPPORTED_TYPES = arrayOf(JOSEObjectType.JWT, JOSEObjectType("at+jwt"), null)
    }

    private class CachedMetadata(val configuredIssuer: String, val metadata: OAuth2ProviderMetadata, val fetchedAt: Long)
    private class CachedKeys(val jwksUri: String, val keys: JWKSet, val fetchedAt: Long)

    private var cachedMetadata: CachedMetadata? = null
    private var cachedKeys: CachedKeys? = null

    @Synchronized
    fun metadata(): Result<OAuth2ProviderMetadata, ErrorResponse> {
        val issuer = settings.map { it.issuer }
        val now = currentTimeMillis()

        cachedMetadata
            ?.takeIf { it.configuredIssuer == issuer && now - it.fetchedAt < METADATA_LIFETIME }
            ?.let { return it.metadata.asSuccess() }

        return providerClient
            .fetchMetadata(issuer)
            .peek { cachedMetadata = CachedMetadata(issuer, it, now) }
    }

    fun verify(token: String, audience: String, nonce: String? = null): Result<JWTClaimsSet, ErrorResponse> =
        metadata().flatMap { metadata ->
            val expectedClaims = JWTClaimsSet.Builder()
                .issuer(metadata.issuer)
                .also { if (nonce != null) it.claim("nonce", nonce) }
                .build()

            val processor = DefaultJWTProcessor<SecurityContext>().also {
                it.jwsTypeVerifier = DefaultJOSEObjectTypeVerifier(*SUPPORTED_TYPES)
                it.jwsKeySelector = JWSVerificationKeySelector(SUPPORTED_ALGORITHMS, keySource(metadata.jwksUri))
                it.jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(audience, expectedClaims, setOf("exp"))
            }

            try {
                processor.process(token, null).asSuccess()
            } catch (exception: Exception) {
                unauthorizedError("Invalid OAuth2 token: ${exception.message}")
            }
        }

    private fun keySource(jwksUri: String): JWKSource<SecurityContext> =
        JWKSource { selector, _ ->
            selector.select(keys(jwksUri, refresh = false)).ifEmpty { selector.select(keys(jwksUri, refresh = true)) }
        }

    @Synchronized
    private fun keys(jwksUri: String, refresh: Boolean): JWKSet {
        val now = currentTimeMillis()
        val cached = cachedKeys?.takeIf { it.jwksUri == jwksUri }
        val age = cached?.let { now - it.fetchedAt }

        if (cached != null && age != null && age < KEYS_LIFETIME && (!refresh || age < KEYS_REFRESH_COOLDOWN)) {
            return cached.keys
        }

        return providerClient
            .fetchKeys(jwksUri)
            .peek { cachedKeys = CachedKeys(jwksUri, it, now) }
            .fold(
                { it },
                { error -> cached?.keys ?: throw KeySourceException(error.message) }
            )
    }

}
