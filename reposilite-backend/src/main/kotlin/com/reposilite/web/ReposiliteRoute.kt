package com.reposilite.web

import com.reposilite.web.routing.Route
import com.reposilite.web.routing.RouteMethod
import com.reposilite.web.routing.Routes

class ReposiliteRoute(
    path: String,
    vararg methods: RouteMethod,
    handler: DslContext.() -> Unit
) : Route<DslContext>(path = path, methods = methods, handler = handler)

interface ReposiliteRoutes : Routes<DslContext>