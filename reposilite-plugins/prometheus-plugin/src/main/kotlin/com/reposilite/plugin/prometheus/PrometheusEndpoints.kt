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