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

package com.reposilite.status.infrastructure

import com.reposilite.web.application.ReposiliteRoute
import com.reposilite.web.application.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.BEFORE

internal class RouteAccessHandler : ReposiliteRoutes() {

    private val collectRequests = ReposiliteRoute("/<*>", BEFORE) {
        logger.debug("${ctx.method()} $uri from ${ctx.ip()}")
    }

    override val routes = setOf(collectRequests)

}