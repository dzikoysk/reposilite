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

package com.reposilite.web

import com.reposilite.journalist.Logger
import com.reposilite.shared.uri
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.RoutePermission
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.error
import io.javalin.http.Context
import io.javalin.http.HttpCode
import panda.std.Result

class ContextDsl(
    val logger: Logger,
    val ctx: Context,
    val authenticationResult: Result<AccessToken, ErrorResponse>
) {

    companion object {

        private val METHOD_PERMISSIONS = mapOf(
            "HEAD" to RoutePermission.READ,
            "GET" to RoutePermission.READ,
            "PUT" to RoutePermission.WRITE,
            "POST" to RoutePermission.WRITE,
            "DELETE" to RoutePermission.WRITE
        )

    }

    val uri = ctx.uri()

    /**
     * Response to send at the end of the dsl call
     */
    var response: Any? = null

    /**
     * Request was created by either anonymous user or through authenticated token
     */
    suspend fun accessed(init: suspend AccessToken?.() -> Unit) {
        init(authenticationResult.orNull())
    }

    /**
     * Request was created by valid access token
     */
    suspend fun authenticated(init: suspend AccessToken.() -> Unit) {
        authenticationResult
            .onError { ctx.error(it) }
            .also {
                if (it.isOk) init(it.get()) // no suspend support in Result#peek
            }
    }

    /**
     * Request was created by valid access token and the token has access to the requested path
     */
    suspend fun authorized(init: suspend AccessToken.() -> Unit) {
        authenticated {
            if (isAuthorized()) {
                init(this)
            } else {
                ctx.error(ErrorResponse(HttpCode.UNAUTHORIZED, "Invalid credentials"))
            }
        }
    }

    fun wildcard(name: String): String? =
        ctx.pathParamMap()[name]

    fun requireParameter(name: String): String =
        parameter(name)!!

    fun parameter(name: String): String? =
        ctx.pathParamMap()[name]

    fun isAuthorized(): Boolean =
        isManager() || authenticationResult.fold({ it.hasPermissionTo(ctx.path(), METHOD_PERMISSIONS[ctx.method()]!!) }, { false })

    fun isManager(): Boolean =
        authenticationResult.fold({ it.hasPermission(AccessTokenPermission.MANAGER) }, { false })

    fun getSessionIdentifier(): String =
        authenticationResult.fold({ "${it.name}@${ctx.ip()}" }, { ctx.ip() })

}