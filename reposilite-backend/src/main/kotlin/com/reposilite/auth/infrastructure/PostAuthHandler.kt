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
package com.reposilite.auth.infrastructure

import com.reposilite.web.ReposiliteRoute
import com.reposilite.web.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.AFTER
import io.javalin.http.HttpCode.UNAUTHORIZED

private const val WWW_AUTHENTICATE = "www-authenticate"
private const val WWW_BASIC_REALM = """Basic realm="Reposilite", charset="UTF-8""""

internal class PostAuthHandler : ReposiliteRoutes() {

    private val realmDescription = ReposiliteRoute("/{repository}/<*>", AFTER) {
        if (ctx.status() == UNAUTHORIZED.status && uri.startsWith("/api").not()) {
            ctx.header(WWW_AUTHENTICATE, WWW_BASIC_REALM)
        }
    }

    override val routes = setOf(realmDescription)

}