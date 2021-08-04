package org.panda_lang.reposilite.maven

import org.panda_lang.reposilite.maven.api.Repository
import org.panda_lang.reposilite.maven.api.RepositoryVisibility.HIDDEN
import org.panda_lang.reposilite.maven.api.RepositoryVisibility.PRIVATE
import org.panda_lang.reposilite.maven.api.RepositoryVisibility.PUBLIC
import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.api.RoutePermission
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