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


@file:Suppress("FunctionName")

package com.reposilite.auth

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.RecommendedRemoteSpecificationJunitExtension
import com.reposilite.auth.api.OAuth2SessionRequest
import com.reposilite.auth.api.OAuth2SessionResponse
import com.reposilite.auth.api.SessionDetails
import com.reposilite.auth.specification.OAuth2IntegrationSpecification
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission.READ
import io.javalin.http.HttpStatus.FOUND
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.NO_CONTENT
import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.UNAUTHORIZED
import kong.unirest.core.Unirest.delete
import kong.unirest.core.Unirest.get
import kong.unirest.core.Unirest.post
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(RecommendedRemoteSpecificationJunitExtension::class)
internal class RemoteOAuth2IntegrationTest : OAuth2IntegrationTest()

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class LocalOAuth2IntegrationTest : OAuth2IntegrationTest()

internal abstract class OAuth2IntegrationTest : OAuth2IntegrationSpecification() {

    @Test
    fun `should sign in through identity provider`() {
        // when: user requests sign in
        val loginResponse = browse("$base/api/auth/oauth2/login")

        // then: user is redirected to the identity provider
        assertThat(loginResponse.statusCode()).isEqualTo(FOUND.code)
        assertThat(loginResponse.location()).startsWith("$issuer/authorize?")
        assertThat(queryOf(loginResponse.location())).containsEntry("redirect_uri", "$base/api/auth/oauth2/callback")

        // when: identity provider authorizes the request
        val authorizationResponse = browse(loginResponse.location())
        // then: user is redirected back to Reposilite
        assertThat(authorizationResponse.location()).startsWith("$base/api/auth/oauth2/callback?")

        // when: callback is requested
        val callbackResponse = browse(authorizationResponse.location())
        // then: user is redirected to dashboard with one-time code
        assertThat(callbackResponse.statusCode()).isEqualTo(FOUND.code)
        assertThat(callbackResponse.location()).startsWith("$base/?oauth2_code=")

        // when: dashboard exchanges the code
        val code = queryOf(callbackResponse.location()).getValue("oauth2_code")
        val session = assertSuccessResponse(OK, post("$base/api/auth/oauth2/session").body(OAuth2SessionRequest(code)).asJacksonObject(OAuth2SessionResponse::class))

        // then: session credentials can be used like access token that has no permissions
        assertThat(session.name).isEqualTo("panda")
        val details = assertSuccessResponse(OK, get("$base/api/auth/me").basicAuth(session.name, session.secret).asJacksonObject(SessionDetails::class))
        assertThat(details.accessToken.name).isEqualTo("panda")
        assertThat(details.accessToken.identifier.type).isEqualTo(PERSISTENT)
        assertThat(details.permissions).isEmpty()
        assertThat(details.routes).isEmpty()

        // and: the code cannot be used again
        assertThat(post("$base/api/auth/oauth2/session").body(OAuth2SessionRequest(code)).asEmpty().status).isEqualTo(UNAUTHORIZED.code)

        // when: session is closed
        val logoutResponse = delete("$base/api/auth/oauth2/session").basicAuth(session.name, session.secret).asEmpty()

        // then: its credentials are rejected
        assertThat(logoutResponse.status).isEqualTo(NO_CONTENT.code)
        assertThat(get("$base/api/auth/me").basicAuth(session.name, session.secret).asEmpty().status).isEqualTo(UNAUTHORIZED.code)
    }

    @Test
    fun `should redirect to dashboard with error if sign in fails`() {
        // when: callback is requested with unknown state
        val callbackResponse = browse("$base/api/auth/oauth2/callback?state=unknown&code=unknown")

        // then: user is redirected to dashboard with error message
        assertThat(callbackResponse.statusCode()).isEqualTo(FOUND.code)
        assertThat(callbackResponse.location()).startsWith("$base/?oauth2_error=")
    }

    @Test
    fun `should authenticate with bearer token`() {
        // given: a private artifact and a token issued by the identity provider
        val (repository, gav, file, content) = useDocument("private", "gav", "artifact.jar", "content", true)
        val jwt = createJwt(username = "ci")

        // when: token is used for the first time
        val details = assertSuccessResponse(OK, get("$base/api/auth/me").header("Authorization", "Bearer $jwt").asJacksonObject(SessionDetails::class))

        // then: access token without permissions has been created
        assertThat(details.accessToken.name).isEqualTo("ci")
        assertThat(details.routes).isEmpty()
        assertThat(get("$base/$repository/$gav/$file").header("Authorization", "Bearer $jwt").asString().isSuccess).isFalse()

        // when: the access token is permitted to read the repository
        useFacade<AccessTokenFacade>().addRoute(details.accessToken.identifier, Route("/$repository", READ))

        // then: artifact can be fetched with token used as bearer token or as a password
        assertThat(get("$base/$repository/$gav/$file").header("Authorization", "Bearer $jwt").asString().body).isEqualTo(content)
        assertThat(get("$base/$repository/$gav/$file").basicAuth("any", jwt).asString().body).isEqualTo(content)
    }

    @Test
    fun `should reject invalid bearer tokens`() {
        listOf(
            createJwt(expiresAt = Instant.now().minusSeconds(600)),
            createJwt(audience = CLIENT_ID),
            createJwt().dropLast(8) + "tampered",
            "not-a-token"
        ).forEach { jwt ->
            assertThat(get("$base/api/auth/me").header("Authorization", "Bearer $jwt").asEmpty().status).isEqualTo(UNAUTHORIZED.code)
        }
    }

    @Test
    fun `should not be available if disabled`() {
        // given: a token accepted by enabled integration
        val jwt = createJwt()
        assertThat(get("$base/api/auth/me").header("Authorization", "Bearer $jwt").asEmpty().status).isEqualTo(OK.code)

        // when: integration is disabled
        useOAuth2Settings { it.copy(enabled = false) }

        // then: endpoints are gone and tokens are rejected
        assertThat(browse("$base/api/auth/oauth2/login").statusCode()).isEqualTo(NOT_FOUND.code)
        assertThat(browse("$base/api/auth/oauth2/callback?state=state&code=code").statusCode()).isEqualTo(NOT_FOUND.code)
        assertThat(post("$base/api/auth/oauth2/session").body(OAuth2SessionRequest("code")).asEmpty().status).isEqualTo(NOT_FOUND.code)
        assertThat(get("$base/api/auth/me").header("Authorization", "Bearer $jwt").asEmpty().status).isEqualTo(UNAUTHORIZED.code)
    }

}
