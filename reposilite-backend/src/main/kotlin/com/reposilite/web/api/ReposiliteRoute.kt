package com.reposilite.web.api

import com.reposilite.web.DslContext
import com.reposilite.web.routing.Route
import com.reposilite.web.routing.RouteMethod
import com.reposilite.web.routing.Routes

class ReposiliteRoute(
    path: String,
    vararg methods: RouteMethod,
    handler: suspend DslContext.() -> Unit
) : Route<DslContext, Unit>(path = path, methods = methods, handler = handler)

abstract class ReposiliteRoutes : Routes<DslContext, Unit>