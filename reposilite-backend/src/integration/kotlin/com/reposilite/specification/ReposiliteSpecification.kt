/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.specification

import com.reposilite.ReposiliteObjectMapper
import com.reposilite.plugin.api.Facade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.FORBIDDEN
import kong.unirest.HttpRequest
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.reflect.KClass

internal abstract class ReposiliteSpecification : ReposiliteRunner() {

    val base: String
        get() = "http://localhost:${reposilite.parameters.port}"

    fun useDefaultManagementToken(): Pair<String, String> =
        "manager" to "manager-secret"

    fun useAuth(name: String, secret: String, permissions: List<AccessTokenPermission> = emptyList(), routes: Map<String, RoutePermission> = emptyMap()): Pair<String, String> {
        val accessTokenFacade = useFacade<AccessTokenFacade>()
        val accessToken = accessTokenFacade.createAccessToken(CreateAccessTokenRequest(PERSISTENT, name, secret = secret)).accessToken

        permissions.forEach {
            accessTokenFacade.addPermission(accessToken.identifier, it)
        }

        routes.forEach { (route, permission) ->
            accessTokenFacade.addRoute(accessToken.identifier, Route(route, permission))
        }

        return name to secret
    }

    inline fun <reified F : Facade> useFacade(): F =
        reposilite.extensions.facade()

    fun <T : Any> HttpRequest<*>.asJacksonObject(type: KClass<T>): HttpResponse<T> =
        this.asObject { ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER.readValue(it.contentAsString, type.java) }

    fun assertStatus(expectedCode: HttpStatus, value: Int) {
        assertEquals(expectedCode.code, value)
    }

    fun assertErrorResponse(expectedCode: HttpStatus, response: HttpResponse<*>) {
        assertStatus(expectedCode, response.status)
        assertFalse(response.isSuccess)
    }

    fun <T> assertSuccessResponse(expectedCode: HttpStatus, response: HttpResponse<T>, block: (T) -> Unit = {}): T {
        assertStatus(expectedCode, response.status)
        assertTrue(response.isSuccess)
        return response.body.also { block(it) }
    }

    fun assertManagerOnlyGetEndpoint(endpoint: String) {
        // given: an existing token without management permission
        val (unauthorizedToken, unauthorizedSecret) = useAuth("unauthorized-token", "secret")

        // when: list of tokens is requested without valid access token
        val unauthorizedResponse = Unirest.get("$base$endpoint")
            .basicAuth(unauthorizedToken, unauthorizedSecret)
            .asJacksonObject(ErrorResponse::class)

        // then: request is rejected
        assertErrorResponse(FORBIDDEN, unauthorizedResponse)
    }

}