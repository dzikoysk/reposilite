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

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.Ticker
import com.reposilite.auth.api.OAuth2SessionResponse
import com.reposilite.token.AccessTokenIdentifier
import org.apache.commons.codec.digest.DigestUtils
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

internal data class OAuth2PendingLogin(
    val codeVerifier: String,
    val nonce: String,
    val redirectUri: String
)

internal data class OAuth2Session(
    val name: String,
    val identifier: AccessTokenIdentifier,
    val duration: Duration
)

/**
 * Keeps short-living state of OAuth2 sign in process and dashboard sessions in memory,
 * so all of them are gone after restart and users have to sign in again.
 */
internal class OAuth2SessionStore(ticker: Ticker = Ticker.systemTicker()) {

    private companion object {
        private val SECURE_RANDOM = SecureRandom()
    }

    private val pendingLogins: Cache<String, OAuth2PendingLogin> =
        Caffeine.newBuilder()
            .ticker(ticker)
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build()

    private val grants: Cache<String, OAuth2SessionResponse> =
        Caffeine.newBuilder()
            .ticker(ticker)
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .build()

    private val sessions: Cache<String, OAuth2Session> =
        Caffeine.newBuilder()
            .ticker(ticker)
            .maximumSize(100_000)
            .expireAfter(Expiry.creating { _: String, session: OAuth2Session -> session.duration })
            .build()

    fun generateSecret(): String =
        ByteArray(32)
            .also { SECURE_RANDOM.nextBytes(it) }
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    fun addPendingLogin(state: String, pendingLogin: OAuth2PendingLogin) =
        pendingLogins.put(state, pendingLogin)

    fun takePendingLogin(state: String): OAuth2PendingLogin? =
        pendingLogins.asMap().remove(state)

    /** Creates session and returns one-time code that can be exchanged for its credentials */
    fun createSession(session: OAuth2Session): String {
        val secret = generateSecret()
        val code = generateSecret()
        sessions.put(hash(secret), session)
        grants.put(code, OAuth2SessionResponse(name = session.name, secret = secret))
        return code
    }

    fun takeGrant(code: String): OAuth2SessionResponse? =
        grants.asMap().remove(code)

    fun findSession(secret: String): OAuth2Session? =
        sessions.getIfPresent(hash(secret))

    fun deleteSession(secret: String) =
        sessions.invalidate(hash(secret))

    private fun hash(secret: String): String =
        DigestUtils.sha256Hex(secret)

}
