/*
 * Copyright (c) 2021 dzikoysk
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

package com.reposilite

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.reposilite.plugin.api.Facade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.Route
import com.reposilite.token.api.RoutePermission
import io.javalin.http.HttpCode
import kong.unirest.HttpRequest
import kong.unirest.HttpResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.reflect.KClass

internal abstract class ReposiliteSpecification : ReposiliteRunner() {

    private val jacksonMapper by lazy {
        JsonMapper.builder()
            .addModule(JavaTimeModule())
            .build()
            .registerKotlinModule()
    }

    val base: String
        get() = "http://localhost:$port"

    fun usePredefinedTemporaryAuth(): Pair<String, String> =
        Pair("manager", "manager-secret")

    fun useAuth(name: String, secret: String, routes: Map<String, RoutePermission> = emptyMap()): Pair<String, String> {
        val accessTokenFacade = useFacade<AccessTokenFacade>()
        var accessToken = accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name, secret)).accessToken

        routes.forEach { (route, permission) ->
            accessToken = accessTokenFacade.updateToken(accessToken.withRoute(Route(route, setOf(permission))))
        }

        return Pair(name, secret)
    }

    inline fun <reified F : Facade> useFacade(): F =
        reposilite.extensionsManagement.facade()

    fun <T : Any> HttpRequest<*>.asJacksonObject(type: KClass<T>): HttpResponse<T> =
        this.asObject { jacksonMapper.readValue(it.contentAsString, type.java) }

    fun assertStatus(expectedCode: HttpCode, value: Int) {
        assertEquals(expectedCode.status, value)
    }

    fun assertErrorResponse(expectedCode: HttpCode, response: HttpResponse<*>) {
        assertStatus(expectedCode, response.status)
        assertFalse(response.isSuccess)
    }

    fun <T> assertSuccessResponse(expectedCode: HttpCode, response: HttpResponse<T>, block: (T) -> Unit = {}): T {
        assertStatus(expectedCode, response.status)
        assertTrue(response.isSuccess)
        return response.body.also { block(it) }
    }

}