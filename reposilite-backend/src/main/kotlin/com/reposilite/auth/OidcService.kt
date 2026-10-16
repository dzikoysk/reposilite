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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.reposilite.auth.application.OidcSettings
import com.reposilite.journalist.Journalist
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.shared.toErrorResponse
import com.reposilite.shared.toErrorResult
import com.reposilite.shared.notFoundError
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.USER_TOKEN_SEPARATOR
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenResponse
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.CONFLICT
import io.javalin.http.HttpStatus.FORBIDDEN
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.HOURS
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val LABEL = Regex("[A-Za-z0-9._-]{1,32}")

data class OidcLoginRequest(val url: String, val state: String, val verifier: String)

data class OidcSession(val name: String, val secret: String)

/**
 * OpenID Connect login for the web interface (authorization code flow with PKCE) and self-service tokens.
 * A user is an access token named after the user (e-mail), its routes are the maximum the user's own tokens may get.
 */
class OidcService(
    private val journalist: Journalist,
    private val settings: Reference<OidcSettings>,
    private val accessTokenFacade: AccessTokenFacade,
) {

    private val http = HttpClient.newHttpClient()
    private val json = ObjectMapper()
    private val random = SecureRandom()
    private val discoveries = ConcurrentHashMap<String, JsonNode>()
    // ponytail: per-process key, a restart signs users out of the web interface; persist it if that becomes annoying
    private val sessionKey = ByteArray(32).also { random.nextBytes(it) }

    fun isEnabled(): Boolean =
        settings.get().enabled

    fun createLoginRequest(): OidcLoginRequest {
        val config = settings.get()
        val state = randomString()
        val verifier = randomString()
        val url = discovery().path("authorization_endpoint").asText() + "?" + form(
            "response_type" to "code",
            "client_id" to config.clientId,
            "redirect_uri" to config.redirectUri,
            "scope" to "openid profile email",
            "state" to state,
            "code_challenge" to base64(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())),
            "code_challenge_method" to "S256",
        )
        return OidcLoginRequest(url, state, verifier)
    }

    /** Logout at the identity provider (RP-initiated logout), null if the provider doesn't support it */
    fun createLogoutUrl(): String? {
        val config = settings.get()
        val endSession = discovery().path("end_session_endpoint").asText().ifBlank { return null }
        return endSession + "?" + form(
            "client_id" to config.clientId,
            "post_logout_redirect_uri" to config.redirectUri.substringBeforeLast("oidc/callback"),
        )
    }

    fun login(code: String, verifier: String): Result<OidcSession, ErrorResponse> {
        val config = settings.get()
        val tokens = post(discovery().path("token_endpoint").asText(), form(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to config.redirectUri,
            "client_id" to config.clientId,
            "client_secret" to config.clientSecret,
            "code_verifier" to verifier,
        ))
        val userinfo = get(discovery().path("userinfo_endpoint").asText(), tokens.path("access_token").asText())
        // some providers (e.g. FusionAuth with strict scope handling) put roles only into the tokens, not into userinfo.
        // both tokens come directly from the token endpoint over TLS (OIDC Core 3.1.3.7), so their claims are only decoded
        val tokenClaims = listOf("id_token", "access_token").mapNotNull { decodeJwtClaims(tokens.path(it).asText()) }
        return login(userinfo, tokenClaims)
    }

    internal fun login(userinfo: JsonNode, tokenClaims: List<JsonNode> = emptyList()): Result<OidcSession, ErrorResponse> {
        val config = settings.get()
        val name = userinfo.path(config.usernameClaim).asText()
        if (name.isBlank() || name.contains(':') || name.contains(USER_TOKEN_SEPARATOR) || name.length > 200)
            return badRequestError("Unsupported '${config.usernameClaim}' claim: '$name' (available claims: ${userinfo.fieldNames().asSequence().joinToString()})")

        val roles = (listOf(userinfo) + tokenClaims)
            .map { it.path(config.rolesClaim) }
            .flatMap { claim -> if (claim.isArray) claim.map { it.asText() } else listOfNotNull(claim.textValue()) }
            .toSet()
        val manager = config.managerRole.isNotBlank() && config.managerRole in roles

        if (config.userRole.isNotBlank() && config.userRole !in roles && !manager) {
            journalist.logger.info("OIDC | $name was refused, roles from '${config.rolesClaim}' claim: $roles")
            return FORBIDDEN.toErrorResult("Your account has no access to Reposilite, ask an administrator for the '${config.userRole}' role")
        }
        journalist.logger.info("OIDC | $name signed in, roles from '${config.rolesClaim}' claim: $roles")

        val user = accessTokenFacade.getAccessToken(name)
            ?: accessTokenFacade.createAccessToken(CreateAccessTokenRequest(type = PERSISTENT, name = name, description = "OIDC user")).accessToken

        if (config.managerRole.isNotBlank() && manager != accessTokenFacade.hasPermission(user.identifier, MANAGER)) {
            // permissions are unique in the database, so only changes are written
            if (manager) accessTokenFacade.addPermission(user.identifier, MANAGER)
            else accessTokenFacade.deletePermission(user.identifier, MANAGER)
        }

        val expiry = Instant.now().plus(config.sessionHours, HOURS).epochSecond
        return OidcSession(name, "$expiry.${sign("$name|$expiry")}").asSuccess()
    }

    /** Validates web interface sessions issued by [login] */
    fun isValidSession(name: String, secret: String): Boolean {
        val (expiry, signature) = secret.split('.').takeIf { it.size == 2 } ?: return false
        return (expiry.toLongOrNull() ?: 0) > Instant.now().epochSecond &&
            MessageDigest.isEqual(sign("$name|$expiry").toByteArray(), signature.toByteArray())
    }

    fun getUserTokens(user: AccessTokenDto): Result<List<AccessTokenDto>, ErrorResponse> =
        requireUser(user).map {
            accessTokenFacade.getAccessTokens()
                .filter { it.name.startsWith("${user.name}$USER_TOKEN_SEPARATOR") }
                .sortedBy { it.name }
        }

    fun createUserToken(user: AccessTokenDto, label: String, routes: Set<CreateAccessTokenRequest.Route>): Result<CreateAccessTokenResponse, ErrorResponse> {
        val name = "${user.name}$USER_TOKEN_SEPARATOR$label"
        val allowedRoutes = accessTokenFacade.getRoutes(user.identifier)

        return requireUser(user)
            .filter({ LABEL.matches(label) }) { BAD_REQUEST.toErrorResponse("Label must have 1-32 characters: letters, digits, '.', '_' or '-'") }
            .filter({ accessTokenFacade.getAccessToken(name) == null }) { CONFLICT.toErrorResponse("Token '$label' already exists") }
            .filter({ routes.all { route -> route.permissions.all { permission -> allowedRoutes.any { it.hasPermissionTo(route.path, permission) } } } }) {
                FORBIDDEN.toErrorResponse("Requested routes exceed the routes of user '${user.name}'")
            }
            .map {
                accessTokenFacade.createAccessToken(
                    CreateAccessTokenRequest(
                        type = PERSISTENT,
                        name = name,
                        routes = routes,
                        expiresAt = Instant.now().plus(settings.get().tokenLifetimeDays, DAYS),
                        description = "Created by ${user.name}",
                    )
                )
            }
    }

    fun deleteUserToken(user: AccessTokenDto, label: String): Result<Unit, ErrorResponse> =
        requireUser(user).flatMap {
            accessTokenFacade.getAccessToken("${user.name}$USER_TOKEN_SEPARATOR$label")
                ?.let { accessTokenFacade.deleteToken(it.identifier) }
                ?: notFoundError("Token '$label' not found")
        }

    /** Tokens created by users can't create further tokens */
    private fun requireUser(user: AccessTokenDto): Result<Unit, ErrorResponse> =
        if (user.name.contains(USER_TOKEN_SEPARATOR)) FORBIDDEN.toErrorResult("Only users can manage their tokens") else Unit.asSuccess()

    private fun decodeJwtClaims(token: String): JsonNode? =
        token.split('.')
            .takeIf { it.size == 3 }
            ?.let { runCatching { json.readTree(Base64.getUrlDecoder().decode(it[1])) }.getOrNull() }

    private fun discovery(): JsonNode {
        val url = settings.get().discoveryUrl.trimEnd('/') + "/.well-known/openid-configuration"
        return discoveries.computeIfAbsent(url) { send(HttpRequest.newBuilder(URI(it))) }
    }

    private fun post(uri: String, body: String): JsonNode =
        send(HttpRequest.newBuilder(URI(uri))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)))

    private fun get(uri: String, bearer: String): JsonNode =
        send(HttpRequest.newBuilder(URI(uri)).header("Authorization", "Bearer $bearer"))

    private fun send(request: HttpRequest.Builder): JsonNode =
        http.send(request.header("Accept", "application/json").build(), BodyHandlers.ofString())
            .also { check(it.statusCode() == 200) { "OIDC request to ${it.uri()} failed (${it.statusCode()}): ${it.body()}" } }
            .let { json.readTree(it.body()) }

    private fun sign(payload: String): String =
        Mac.getInstance("HmacSHA256")
            .apply { init(SecretKeySpec(sessionKey, "HmacSHA256")) }
            .doFinal(payload.toByteArray())
            .let { base64(it) }

    private fun randomString(): String =
        ByteArray(32).also { random.nextBytes(it) }.let { base64(it) }

    private fun base64(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun form(vararg params: Pair<String, String>): String =
        params.joinToString("&") { (key, value) -> key + "=" + URLEncoder.encode(value, Charsets.UTF_8) }

}
