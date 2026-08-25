/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.plugin.prometheus

import com.reposilite.shared.unauthorizedError
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.GET
import io.javalin.http.Header
import java.io.InputStream

internal class PrometheusEndpoints(
    private val prometheusFacade: PrometheusFacade,
    prometheusPath: String,
) : ReposiliteRoutes() {

    private val getMetrics = ReposiliteRoute<InputStream>(prometheusPath, GET) {
        response = ctx.basicAuthCredentials()
            ?.takeIf { prometheusFacade.hasAccess(it.username, it.password) }
            ?.let {
                prometheusFacade.getMetrics(
                    acceptedType = ctx.header(Header.ACCEPT),
                    names = ctx.queryParams("name[]").toSet()
                )
            }
            ?.peek { ctx.contentType(it.contentType) }
            ?.map { it.content }
            ?: unauthorizedError("Invalid credentials")
    }

    override val routes = routes(getMetrics)

}