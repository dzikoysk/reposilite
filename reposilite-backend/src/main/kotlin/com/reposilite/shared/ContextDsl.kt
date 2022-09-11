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

package com.reposilite.shared

import com.reposilite.journalist.Logger
import com.reposilite.shared.extensions.uri
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.RoutePermission
import com.reposilite.token.api.AccessTokenDto
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.HttpStatus.FORBIDDEN
import panda.std.Result
import panda.std.asError
import panda.std.mapToUnit

class ContextDsl<R>(
    val logger: Logger,
    val ctx: Context,
    private val accessTokenFacade: AccessTokenFacade,
    private val authenticationResult: Lazy<Result<out AccessTokenDto, ErrorResponse>>
) {

    private companion object {
        private val METHOD_PERMISSIONS = mapOf(
            HandlerType.HEAD to RoutePermission.READ,
            HandlerType.GET to RoutePermission.READ,
            HandlerType.PUT to RoutePermission.WRITE,
            HandlerType.POST to RoutePermission.WRITE,
            HandlerType.DELETE to RoutePermission.WRITE
        )
    }

    val uri = ctx.uri()

    /**
     * Response to send at the end of the dsl call
     */
    var response: Result<out R, ErrorResponse>? = null

    /**
     * Request was created by either anonymous user or through authenticated token
     */
    fun accessed(init: AccessTokenDto?.() -> Unit) {
        init(authenticationResult.value.orNull())
    }

    /**
     * Request was created by valid access token
     */
    fun authenticated(init: AccessTokenDto.() -> Unit) {
        authenticationResult.value
            .peek { init(it) }
            .onError { response = it.asError() }
    }

    /**
     * Request was created by valid access token and the token has access to the requested path
     */
    fun authorized(to: String = ctx.uri(), init: AccessTokenDto.() -> Unit) {
        authenticated {
            isAuthorized(to)
                .peek { init(this) }
                .onError { response = it.asError() }
        }
    }

    /**
     * Request was created with manager access token
     */
    fun managerOnly(block: AccessTokenDto.() -> Unit) {
        authenticated {
            isManager()
                .peek { block(this) }
                .onError { response = it.asError() }
        }
    }

    fun wildcard(name: String): String? =
        ctx.pathParamMap()[name]

    fun requireParameter(name: String): String =
        parameter(name)!!

    fun parameter(name: String): String? =
        ctx.pathParamMap()[name]

    fun queryParameter(name: String): String? =
        ctx.queryParam(name)

    inline fun <reified T : Any> body() =
        ctx.bodyAsClass(T::class.java)

    private fun isAuthorized(to: String): Result<Unit, ErrorResponse> =
        isManager()
            .flatMapErr { _ ->
                authenticationResult.value
                    .filter(
                        { accessTokenFacade.hasPermissionTo(it.identifier, to, METHOD_PERMISSIONS[ctx.method()]!!) },
                        { FORBIDDEN.toErrorResponse("This token is not authorized to access this path") }
                    )
                    .mapToUnit()
            }

    fun isManager(): Result<Unit, ErrorResponse> =
        authenticationResult.value
            .filter(
                { accessTokenFacade.hasPermission(it.identifier, MANAGER) },
                { FORBIDDEN.toErrorResponse("Only manager can access this endpoint") }
            )
            .mapToUnit()

    fun getSessionIdentifier(): String =
        authenticationResult.value.fold({ "${it.name}@${ctx.ip()}" }, { ctx.ip() })

    fun authentication(): Result<out AccessTokenDto, ErrorResponse> =
        authenticationResult.value

}
