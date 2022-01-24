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
            response = javadoc.findJavadocPage(JavadocPageRequest(requireParameter("repository"), requireParameter("gav").toLocation(), this))
                .peek { ctx.encoding(Charsets.UTF_8).contentType(it.contentType) }
                .map { it.response }
        }
    }

    override val routes = routes(javadocRoute)

}