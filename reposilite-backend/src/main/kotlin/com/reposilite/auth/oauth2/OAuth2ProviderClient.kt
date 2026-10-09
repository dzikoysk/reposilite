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

import com.nimbusds.jose.jwk.JWKSet
import com.reposilite.ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.toErrorResponse
import com.reposilite.shared.toErrorResult
import io.javalin.http.HttpStatus.BAD_GATEWAY
import panda.std.Result
import panda.std.asSuccess
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import java.util.Base64

internal data class OAuth2ProviderMetadata(
    val issuer: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val jwksUri: String
)

internal data class OAuth2CodeExchangeRequest(
    val tokenEndpoint: String,
    val clientId: String,
    val clientSecret: String,
    val code: String,
    val redirectUri: String,
    val codeVerifier: String
)

internal interface OAuth2ProviderClient {

    fun fetchMetadata(issuer: String): Result<OAuth2ProviderMetadata, ErrorResponse>

    fun fetchKeys(jwksUri: String): Result<JWKSet, ErrorResponse>

    /** Exchanges authorization code for the ID token of authenticated user */
    fun exchangeCode(request: OAuth2CodeExchangeRequest): Result<String, ErrorResponse>

}

internal fun String.urlEncoded(): String =
    URLEncoder.encode(this, UTF_8)

internal class HttpOAuth2ProviderClient : OAuth2ProviderClient {

    private companion object {
        private val TIMEOUT = Duration.ofSeconds(10)
    }

    private val httpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build()
    }

    override fun fetchMetadata(issuer: String): Result<OAuth2ProviderMetadata, ErrorResponse> =
        get("${issuer.trimEnd('/')}/.well-known/openid-configuration")
            .flatMap { body ->
                request("read OAuth2 provider metadata") {
                    val json = DEFAULT_OBJECT_MAPPER.readTree(body)

                    OAuth2ProviderMetadata(
                        issuer = json.required("issuer").asText(),
                        authorizationEndpoint = json.required("authorization_endpoint").asText(),
                        tokenEndpoint = json.required("token_endpoint").asText(),
                        jwksUri = json.required("jwks_uri").asText()
                    )
                }
            }
            .filter(
                { it.issuer.trimEnd('/') == issuer.trimEnd('/') },
                { BAD_GATEWAY.toErrorResponse("OAuth2 provider metadata has been issued by ${it.issuer} instead of $issuer") }
            )

    override fun fetchKeys(jwksUri: String): Result<JWKSet, ErrorResponse> =
        get(jwksUri).flatMap { body ->
            request("read OAuth2 provider keys") { JWKSet.parse(body) }
        }

    override fun exchangeCode(request: OAuth2CodeExchangeRequest): Result<String, ErrorResponse> {
        val form = linkedMapOf(
            "grant_type" to "authorization_code",
            "code" to request.code,
            "redirect_uri" to request.redirectUri,
            "code_verifier" to request.codeVerifier
        )

        val httpRequest = HttpRequest.newBuilder(URI.create(request.tokenEndpoint))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")

        when {
            // Public clients identify themselves in the body, confidential ones use client_secret_basic
            request.clientSecret.isEmpty() -> form["client_id"] = request.clientId
            else -> {
                val credentials = "${request.clientId.urlEncoded()}:${request.clientSecret.urlEncoded()}"
                httpRequest.header("Authorization", "Basic ${Base64.getEncoder().encodeToString(credentials.toByteArray(UTF_8))}")
            }
        }

        val body = form.entries.joinToString("&") { (key, value) -> "${key.urlEncoded()}=${value.urlEncoded()}" }

        return send(httpRequest.POST(BodyPublishers.ofString(body)).build())
            .flatMap { response ->
                request("read OAuth2 token response") {
                    DEFAULT_OBJECT_MAPPER.readTree(response).required("id_token").asText()
                }
            }
    }

    private fun get(uri: String): Result<String, ErrorResponse> =
        request("create request to $uri") {
            HttpRequest.newBuilder(URI.create(uri))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build()
        }.flatMap { send(it) }

    private fun send(httpRequest: HttpRequest): Result<String, ErrorResponse> =
        request("connect to ${httpRequest.uri()}") { httpClient.send(httpRequest, BodyHandlers.ofString()) }
            .flatMap { response ->
                when (response.statusCode()) {
                    in 200..299 -> response.body().asSuccess()
                    else -> BAD_GATEWAY.toErrorResult("OAuth2 provider responded with ${response.statusCode()} to ${httpRequest.uri()}")
                }
            }

    private fun <T : Any> request(action: String, block: () -> T): Result<T, ErrorResponse> =
        try {
            Result.ok(block())
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            BAD_GATEWAY.toErrorResult("Interrupted attempt to $action")
        } catch (exception: Exception) {
            BAD_GATEWAY.toErrorResult("Cannot $action: ${exception.message}")
        }

}
