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
package org.panda_lang.reposilite.auth.infrastructure

import io.javalin.http.Context
import io.javalin.http.HttpCode
import org.panda_lang.reposilite.web.RouteHandler
import org.panda_lang.reposilite.web.RouteMethod.AFTER

private const val WWW_AUTHENTICATE = "www-authenticate"
private const val WWW_BASIC_REALM = """Basic realm="Reposilite", charset="UTF-8" """

internal class PostAuthHandler : RouteHandler {

    override val route = "/*"
    override val methods = listOf(AFTER)

    override fun handle(context: Context) {
        if (context.status() == HttpCode.UNAUTHORIZED.status) {
            context.header(WWW_AUTHENTICATE, WWW_BASIC_REALM)
        }
    }

}