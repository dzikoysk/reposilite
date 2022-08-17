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

package com.reposilite.web.api

import com.reposilite.shared.ContextDsl
import com.reposilite.web.routing.RouteMethod
import com.reposilite.web.routing.Routes
import com.reposilite.web.routing.StandardRoute

abstract class ReposiliteRoutes : Routes<ReposiliteRoute<Any>> {

    @Suppress("UNCHECKED_CAST")
    fun routes(vararg reposiliteRoutes: ReposiliteRoute<*>): Set<ReposiliteRoute<Any>> =
        reposiliteRoutes
            .map { it as ReposiliteRoute<Any> }
            .toSet()

}

class ReposiliteRoute<R>(
    path: String,
    vararg methods: RouteMethod,
    handler: ContextDsl<R>.() -> Unit
) : StandardRoute<ContextDsl<R>, Unit>(path = path, methods = methods, handler = handler)
