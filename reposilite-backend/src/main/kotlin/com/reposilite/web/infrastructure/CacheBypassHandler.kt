package com.reposilite.web.infrastructure

import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.BEFORE

internal class CacheBypassHandler : ReposiliteRoutes() {

    private val bypassCacheRoute = ReposiliteRoute<Unit>("*", BEFORE) {
        ctx.header("pragma", "no-cache")
        ctx.header("expires", "0")
        ctx.header("cache-control", "no-cache, no-store, must-revalidate, max-age=0")
    }

    override val routes = routes(bypassCacheRoute)

}
