package com.reposilite.web.api

import com.reposilite.Reposilite

class RoutingSetupEvent(val reposilite: Reposilite) {

    private val routes: MutableSet<ReposiliteRoutes> = mutableSetOf()

    fun registerRoutes(routesToAdd: ReposiliteRoutes) {
        routes.add(routesToAdd)
    }

    fun registerRoutes(routesToAdd: Set<ReposiliteRoutes>) {
        routes.addAll(routesToAdd)
    }

    @Suppress("REDUNDANT_PROJECTION")
    fun getRoutes(): Collection<out ReposiliteRoutes> =
        routes

}