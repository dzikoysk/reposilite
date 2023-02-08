/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.web.infrastructure

import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.BEFORE

internal class CacheBypassHandler : ReposiliteRoutes() {

    private val bypassCacheRoute = ReposiliteRoute<Unit>("/api/*", BEFORE) {
        ctx.header("pragma", "no-cache")
        ctx.header("expires", "0")
        ctx.header("cache-control", "no-cache, no-store, must-revalidate, max-age=0")
    }

    override val routes = routes(bypassCacheRoute)

}
