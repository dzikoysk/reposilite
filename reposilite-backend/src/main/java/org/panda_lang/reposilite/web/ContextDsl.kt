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

package org.panda_lang.reposilite.web

import io.javalin.http.Context
import org.apache.http.HttpStatus
import org.panda_lang.reposilite.auth.Session
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.utilities.commons.function.Result

class ContextDsl(
    private val ctx: Context,
    val context: ReposiliteContext
) {

    /**
     * JSON response to send at the end of the dsl call
     */
    var response: Result<out Any?, ErrorResponse>? = null

    /**
     * Request was created by valid access token
     */
    fun authenticated(init: Session.() -> Unit) {
        context.session
            .onError { ctx.error(it) }
            .peek { init.invoke(it) }
    }

    /**
     * Request was created by valid access token and the token has access to the requested path
     */
    fun authorized(init: Session.() -> Unit) {
        authenticated {
            if (isAuthorized()) {
                init.invoke(this)
            } else {
                ctx.error(ErrorResponse(HttpStatus.SC_UNAUTHORIZED, ""))
            }
        }
    }

    fun wildcard(): String =
        ctx.splat(0) ?: ""

    fun parameter(name: String): String =
        ctx.pathParam(name)

    internal fun handleResult(result: Result<out Any?, ErrorResponse>?) {
        result
            ?.mapErr { ctx.json(it) }
            ?.map { it?.let { ctx.json(it) } }
    }

}

fun context(contextFactory: ReposiliteContextFactory, ctx: Context, init: ContextDsl.() -> Unit): Unit =
    contextFactory.create(ctx)
        .onError { ctx.json(it) }
        .map { ContextDsl(ctx, it) }
        .peek { init.invoke(it) }
        .peek { it.handleResult(it.response) }
        .end()