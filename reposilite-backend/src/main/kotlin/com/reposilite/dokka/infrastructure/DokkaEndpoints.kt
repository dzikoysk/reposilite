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

package com.reposilite.dokka.infrastructure

import com.reposilite.dokka.DokkaFacade
import com.reposilite.dokka.api.DokkaPageRequest
import com.reposilite.dokka.api.DokkaRawRequest
import com.reposilite.maven.infrastructure.MavenRoutes
import com.reposilite.shared.extensions.encoding
import com.reposilite.storage.api.toLocation
import com.reposilite.web.api.ReposiliteRoute
import io.javalin.community.routing.Route.GET
import io.javalin.http.Header

internal class DokkaEndpoints(dokka: DokkaFacade) : MavenRoutes(dokka.mavenFacade) {

    private val dokkaRoute = ReposiliteRoute<Any>("/dokka/{repository}/<gav>", GET) {
        accessed {
            requireGav { gav ->
                requireRepository { repository ->
                    when {
                        uri.endsWith("/") -> ctx.redirect(uri.dropLast(1))
                        else -> {
                            response = DokkaPageRequest(this?.identifier, repository, gav)
                                .let { dokka.findDokkaPage(it) }
                                .peek { ctx.encoding(Charsets.UTF_8).contentType(it.contentType) }
                                .map { it.content }
                        }
                    }
                }
            }
        }
    }

    private val dokkaRawRoute = ReposiliteRoute<Any>("/dokka/{repository}/<gav>/raw/<resource>", GET) {
        accessed {
            requireGav { gav ->
                requireRepository { repository ->
                    response = DokkaRawRequest(this?.identifier, repository, gav, requireParameter("resource").toLocation())
                        .let { dokka.findRawDokkaResource(it) }
                        .peek { ctx.encoding(Charsets.UTF_8).contentType(it.contentType).header(Header.CONTENT_SECURITY_POLICY, "sandbox allow-scripts allow-same-origin") }
                        .map { it.content }
                }
            }
        }
    }

    override val routes = routes(dokkaRoute, dokkaRawRoute)

}