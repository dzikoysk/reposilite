package com.reposilite.web.routing

@Deprecated("Use io.javalin.community.routing.Route instead")
enum class RouteMethod(val isHttpMethod: Boolean = true) {
    HEAD,
    PATCH,
    OPTIONS,
    GET,
    PUT,
    POST,
    DELETE,
    AFTER(isHttpMethod = false),
    BEFORE(isHttpMethod = false)
}