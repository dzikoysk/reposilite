package com.reposilite.plugin.javadoc

import com.reposilite.shared.ContextDsl
import com.reposilite.shared.fs.getExtension
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.encoding
import com.reposilite.web.routing.Route
import com.reposilite.web.routing.RouteMethod

class JavadocRoute(javadoc: JavadocFacade) : ReposiliteRoutes() {

    private val docRoute = ReposiliteRoute("/javadoc/{repository}/<gav>", RouteMethod.GET) {
        accessed {
            ctx.encoding(Charsets.UTF_8)

            javadoc.resolveRequest(requiredParameter("repository"), requiredParameter("gav"), uri.getExtension(), this)
                .consume(
                    {
                        ctx.contentType(it.contentType)
                        response = it.response
                    },
                    {
                        response = it
                    }
                )
        }
    }

    override val routes: Set<Route<ContextDsl, Unit>> = setOf(docRoute)
}