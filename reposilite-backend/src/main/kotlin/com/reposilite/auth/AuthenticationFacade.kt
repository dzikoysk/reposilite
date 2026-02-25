/*
 * Copyright (c) 2023 dzikoysk
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

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.reposilite.auth.api.Credentials
import com.reposilite.auth.api.SessionDetails
import com.reposilite.auth.application.BruteForceProtectionSettings
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFoundError
import com.reposilite.shared.unauthorizedError
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.api.AccessTokenDto
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference
import java.util.SortedSet
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

private data class FailedAttempt(
    val count: Int,
    val firstFailureMillis: Long
)

class AuthenticationFacade(
    private val journalist: Journalist,
    private val authenticators: SortedSet<Authenticator>,
    private val accessTokenFacade: AccessTokenFacade,
    private val bruteForceProtectionSettings: Reference<BruteForceProtectionSettings> = Reference.reference(BruteForceProtectionSettings())
) : Journalist, Facade {

    private val authenticationCache: Cache<Credentials, AccessTokenDto> =
        CacheBuilder
            .newBuilder()
            .maximumSize(16)
            .expireAfterAccess(1, MINUTES)
            .build()

    private val failedAttempts: Cache<String, FailedAttempt> =
        CacheBuilder
            .newBuilder()
            .maximumSize(1024)
            .expireAfterWrite(1, HOURS)
            .build()

    fun registerAuthenticator(authenticator: Authenticator) {
        this.authenticators.add(authenticator)
    }

    fun authenticateByCredentials(credentials: Credentials): Result<out AccessTokenDto, ErrorResponse> {
        if (isBruteForceBlocked(credentials.host)) {
            return unauthorizedError("Invalid authorization credentials")
        }

        val result =
            authenticationCache
                .getIfPresent(credentials)
                ?.asSuccess()
                ?: authenticators
                    .asSequence()
                    .filter { it.enabled() }
                    .map { authenticator ->
                        authenticator
                            .authenticate(credentials)
                            .onError { logger.debug("${credentials.name}@${credentials.host} failed to authenticate with ${authenticator.realm()} realm due to $it") }
                    }
                    .firstOrNull { it.isOk }
                    ?.peek { authenticationCache.put(credentials, it) }
                ?: unauthorizedError("Invalid authorization credentials")

        trackBruteForceAttempt(ip = credentials.host, success = result.isOk)
        return result
    }

    private fun isBruteForceBlocked(ip: String): Boolean {
        if (bruteForceProtectionSettings.map { !it.enabled }) {
            return false
        }

        val attempt = failedAttempts.getIfPresent(ip) ?: return false

        if (attempt.count < bruteForceProtectionSettings.map { it.maxAttempts }) {
            return false
        }

        if (System.currentTimeMillis() - attempt.firstFailureMillis < bruteForceProtectionSettings.map { it.banDurationSeconds } * 1000) {
            return true
        }

        failedAttempts.invalidate(ip)
        return false
    }

    private fun trackBruteForceAttempt(ip: String, success: Boolean) {
        if (bruteForceProtectionSettings.map { !it.enabled }) {
            return
        }

        if (success) {
            failedAttempts.invalidate(ip)
            return
        }

        val current = failedAttempts.getIfPresent(ip)
        val updated =
            FailedAttempt(
                count = (current?.count ?: 0) + 1,
                firstFailureMillis = current?.firstFailureMillis ?: System.currentTimeMillis()
            )
        failedAttempts.put(ip, updated)

        if (updated.count == bruteForceProtectionSettings.map { it.maxAttempts }) {
            logger.warn("Brute force protection: IP $ip has been blocked after ${bruteForceProtectionSettings.map { it.maxAttempts }} failed authentication attempts")
        }
    }

    fun getSessionDetails(identifier: AccessTokenIdentifier): Result<SessionDetails, ErrorResponse> =
        accessTokenFacade.getAccessTokenById(identifier)
            ?.let {
                SessionDetails(
                    accessToken = it,
                    permissions = accessTokenFacade.getPermissions(it.identifier),
                    routes = accessTokenFacade.getRoutes(it.identifier)
                )
            }
            ?.asSuccess()
            ?: notFoundError("Token $identifier not found")

    override fun getLogger(): Logger =
        journalist.logger

}
