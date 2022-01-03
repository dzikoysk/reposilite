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

package com.reposilite.shared

import com.reposilite.journalist.Logger
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.RoutePermission
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.error
import com.reposilite.web.http.unauthorized
import com.reposilite.web.http.uri
import io.javalin.http.Context
import panda.std.Result

class ContextDsl(
    val logger: Logger,
    val ctx: Context,
    val authenticationResult: Lazy<Result<AccessToken, ErrorResponse>>
) {

    val uri = ctx.uri()

    /**
     * Response to send at the end of the dsl call
     */
    var response: Any? = null

    /**
     * Request was created by either anonymous user or through authenticated token
     */
    fun accessed(init: AccessToken?.() -> Unit) {
        init(authenticationResult.value.orNull())
    }

    /**
     * Request was created by valid access token
     */
    fun authenticated(init: AccessToken.() -> Unit) {
        authenticationResult.value
            .onError { ctx.error(it) }
            .peek { init(it) }
    }

    /**
     * Request was created by valid access token and the token has access to the requested path
     */
    fun authorized(to: String = ctx.uri(), init: AccessToken.() -> Unit) {
        authenticated {
            if (isAuthorized(to))
                init(this)
            else
                response = unauthorized("Invalid credentials")
        }
    }

    /**
     * Request was created with manager access token
     */
    fun managerOnly(block: AccessToken.() -> Unit) {
        authenticated {
            if (isManager())
                block(this)
            else
                response = unauthorized("Only manager can access this endpoint")
        }
    }

    fun wildcard(name: String): String? =
        ctx.pathParamMap()[name]

    fun requireParameter(name: String): String =
        parameter(name)!!

    fun parameter(name: String): String? =
        ctx.pathParamMap()[name]

    fun isAuthorized(to: String): Boolean =
        isManager() || authenticationResult.value.fold({ it.hasPermissionTo(to, METHOD_PERMISSIONS[ctx.method()]!!) }, { false })

    fun isManager(): Boolean =
        authenticationResult.value.fold({ it.hasPermission(AccessTokenPermission.MANAGER) }, { false })

    fun getSessionIdentifier(): String =
        authenticationResult.value.fold({ "${it.name}@${ctx.ip()}" }, { ctx.ip() })

    private companion object {
        private val METHOD_PERMISSIONS = mapOf(
            "HEAD" to RoutePermission.READ,
            "GET" to RoutePermission.READ,
            "PUT" to RoutePermission.WRITE,
            "POST" to RoutePermission.WRITE,
            "DELETE" to RoutePermission.WRITE
        )
    }

}