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

import com.reposilite.auth.Session
import com.reposilite.web.context.error
import com.reposilite.web.error.ErrorResponse
import io.javalin.http.Context
import io.javalin.http.HttpCode
import panda.std.Result

class DslContext(val ctx: Context, val context: ReposiliteContext) {

    /**
     * JSON response to send at the end of the dsl call
     */
    var response: Result<out Any?, ErrorResponse>? = null

    /**
     * Request was created by either anonymous user or through authenticated token
     */
    fun accessed(init: Session?.() -> Unit) {
        init(context.session.orElseGet { null })
    }

    /**
     * Request was created by valid access token
     */
    fun authenticated(init: Session.() -> Unit) {
        context.session
            .onError { ctx.error(it) }
            .peek { init(it) }
    }

    /**
     * Request was created by valid access token and the token has access to the requested path
     */
    fun authorized(init: Session.() -> Unit) {
        authenticated {
            if (isAuthorized()) {
                init(this)
            } else {
                ctx.error(ErrorResponse(HttpCode.UNAUTHORIZED, ""))
            }
        }
    }

    fun parameter(name: String): String =
        ctx.pathParam(name)

    internal fun handleResult(result: Result<out Any?, ErrorResponse>?) {
        result
            ?.mapErr { ctx.json(it) }
            ?.map { it?.let { ctx.json(it) } }
    }

}

fun context(contextFactory: ReposiliteContextFactory, ctx: Context, init: DslContext.() -> Unit) {
    DslContext(ctx, contextFactory.create(ctx))
        .also { init(it) }
        .also { it.handleResult(it.response) }
}
