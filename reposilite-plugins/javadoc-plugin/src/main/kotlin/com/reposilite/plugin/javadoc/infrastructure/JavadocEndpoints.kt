package com.reposilite.plugin.javadoc.infrastructure

import com.reposilite.plugin.javadoc.JavadocFacade
import com.reposilite.plugin.javadoc.api.ResolveJavadocRequest
import com.reposilite.shared.ContextDsl
import com.reposilite.storage.api.toLocation
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.encoding
import com.reposilite.web.routing.Route
import com.reposilite.web.routing.RouteMethod

class JavadocEndpoints(javadoc: JavadocFacade) : ReposiliteRoutes() {

    private val docRoute = ReposiliteRoute<Any>("/javadoc/{repository}/<gav>", RouteMethod.GET) {
        accessed {
            response = javadoc.resolveRequest(ResolveJavadocRequest(requireParameter("repository"), requireParameter("gav").toLocation(), this))
                .peek { ctx.encoding(Charsets.UTF_8).contentType(it.contentType) }
                .map { it.response }
        }
    }

    override val routes: Set<Route<ContextDsl<*>, Unit>> = routes(docRoute)

}