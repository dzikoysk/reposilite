package com.reposilite.plugin.javadoc

import com.reposilite.shared.ContextDsl
import com.reposilite.storage.api.Location
import com.reposilite.storage.getExtension
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.encoding
import com.reposilite.web.routing.Route
import com.reposilite.web.routing.RouteMethod
import panda.std.Result

class JavadocRoute(javadoc: JavadocFacade) : ReposiliteRoutes() {

    private val docRoute = ReposiliteRoute<Any>("/javadoc/{repository}/<gav>", RouteMethod.GET) {
        accessed {
            ctx.encoding(Charsets.UTF_8)

            javadoc.resolveRequest(requireParameter("repository"), Location.Companion.of(requireParameter("gav")), uri.getExtension(), this)
                .consume(
                    {
                        ctx.contentType(it.contentType)
                        response = Result.ok(it.response as Any)
                    },
                    {
                        response = Result.error(it)
                    }
                )
        }
    }

    override val routes: Set<Route<ContextDsl<*>, Unit>> = routes(docRoute)
}