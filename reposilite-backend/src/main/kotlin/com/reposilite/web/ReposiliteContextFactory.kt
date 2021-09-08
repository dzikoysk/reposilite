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

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.SessionMethod
import com.reposilite.journalist.Journalist
import com.reposilite.web.http.errorResponse
import io.javalin.http.Context
import io.javalin.http.HttpCode.UNAUTHORIZED
import io.javalin.websocket.WsContext
import kotlinx.coroutines.CoroutineDispatcher

class ReposiliteContextFactory internal constructor(
    private val journalist: Journalist,
    private val dispatcher: CoroutineDispatcher,
    private val forwardedIpHeader: String,
    private val authenticationFacade: AuthenticationFacade
) {

    suspend fun create(context: Context): ReposiliteContext {
        val uri = context.req.requestURI
        val host = context.header(forwardedIpHeader) ?: context.req.remoteAddr

        val session = authenticationFacade.authenticateByHeader(context.headerMap())
            .map { authenticationFacade.createSession(uri, SessionMethod.valueOf(context.method().uppercase()), host, it) }

        return ReposiliteContext(
            journalist,
            dispatcher,
            uri,
            context.method(),
            host,
            context.headerMap(),
            session,
            lazy { context.body() },
            { context.req.inputStream }
        )
    }

    fun create(context: WsContext): ReposiliteContext {
        return ReposiliteContext(
            journalist,
            dispatcher,
            context.host(),
            "SOCKET",
            context.header(forwardedIpHeader) ?: context.session.remoteAddress.toString(),
            context.headerMap(),
            errorResponse(UNAUTHORIZED, "WebSocket based context does not support sessions"),
            lazy { throw UnsupportedOperationException("WebSocket based context does not support input stream") },
            { throw UnsupportedOperationException("WebSocket based context does not support input stream") }
        )
    }

}