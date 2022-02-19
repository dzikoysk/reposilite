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

package com.reposilite.plugin.javadoc.infrastructure

import com.reposilite.plugin.javadoc.JavadocFacade
import com.reposilite.plugin.javadoc.api.JavadocPageRequest
import com.reposilite.storage.api.toLocation
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.encoding
import com.reposilite.web.routing.RouteMethod

internal class JavadocEndpoints(javadoc: JavadocFacade) : ReposiliteRoutes() {

    private val javadocRoute = ReposiliteRoute<Any>("/javadoc/{repository}/<gav>", RouteMethod.GET) {
        accessed {
            response = javadoc.findJavadocPage(JavadocPageRequest(this?.identifier, requireParameter("repository"), requireParameter("gav").toLocation()))
                .peek { ctx.encoding(Charsets.UTF_8).contentType(it.contentType) }
                .map { it.response }
        }
    }

    override val routes = routes(javadocRoute)

}