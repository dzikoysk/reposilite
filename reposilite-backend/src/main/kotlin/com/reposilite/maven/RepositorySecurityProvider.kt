package com.reposilite.maven

import com.reposilite.maven.api.Repository
import com.reposilite.maven.api.RepositoryVisibility.HIDDEN
import com.reposilite.maven.api.RepositoryVisibility.PRIVATE
import com.reposilite.maven.api.RepositoryVisibility.PUBLIC
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.RoutePermission
import java.nio.file.Path

internal class RepositorySecurityProvider {

    fun canAccessResource(accessToken: AccessToken?, repository: Repository, gav: Path): Boolean =
        when (repository.visibility) {
            PUBLIC -> true
            HIDDEN -> true
            PRIVATE -> hasPermissionTo(accessToken, repository, gav)
        }

    fun canBrowseResource(accessToken: AccessToken?, repository: Repository, gav: Path): Boolean =
        when (repository.visibility) {
            PUBLIC -> true
            HIDDEN -> hasPermissionTo(accessToken, repository, gav)
            PRIVATE -> hasPermissionTo(accessToken, repository, gav)
        }

    private fun hasPermissionTo(accessToken: AccessToken?, repository: Repository, gav: Path): Boolean =
        accessToken?.hasPermissionTo("/" + repository.name + "/" + gav.toString().replace("\\", "/"), RoutePermission.READ) ?: false

}