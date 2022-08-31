package com.reposilite.maven.infrastructure

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.Repository
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.notFoundError
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.toLocation
import com.reposilite.web.api.ReposiliteRoutes

abstract class MavenRoutes(val mavenFacade: MavenFacade) : ReposiliteRoutes() {

    fun <R> ContextDsl<R>.repository(block: (Repository?) -> Unit) {
        val repository = parameter("repository") ?: run {
            response = notFoundError("Missing repository parameter")
            return
        }

        block(mavenFacade.getRepository(repository))
    }

    fun <R> ContextDsl<R>.requireRepository(block: (Repository) -> Unit) {
        repository {
            when (it) {
                null -> response = notFoundError("Repository not found")
                else -> block(it)
            }
        }
    }

    fun <R> ContextDsl<R>.requireGav(block: (Location) -> Unit) =
        block(requireParameter("gav").toLocation())

}
