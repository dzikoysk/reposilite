/*
 * Copyright (c) 2020 Dzikoysk
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
package org.panda_lang.reposilite

import io.javalin.http.Context
import io.javalin.websocket.WsContext
import net.dzikoysk.dynamiclogger.Journalist

@Suppress("MoveLambdaOutsideParentheses")
class ReposiliteContextFactory internal constructor(
    private val journalist: Journalist,
    private val forwardedIpHeader: String
) {

    fun create(context: Context): ReposiliteContext {
        return ReposiliteContext(
            journalist,
            context.req.requestURI,
            context.method(),
            context.header(forwardedIpHeader) ?: context.req.remoteAddr,
            context.headerMap(),
            null,
            { context.req.inputStream }
        )
    }

    fun create(context: WsContext): ReposiliteContext {
        return ReposiliteContext(
            journalist,
            context.host(),
            "WS",
            context.header(forwardedIpHeader) ?: context.session.remoteAddress.toString(),
            context.headerMap(),
            null,
            { throw UnsupportedOperationException("WebSocket based context does not support input stream") }
        )
    }

}