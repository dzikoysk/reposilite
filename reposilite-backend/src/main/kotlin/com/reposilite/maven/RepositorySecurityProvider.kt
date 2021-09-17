/*
 * Copyright (c) 2021 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.maven

import com.reposilite.maven.api.RepositoryVisibility.HIDDEN
import com.reposilite.maven.api.RepositoryVisibility.PRIVATE
import com.reposilite.maven.api.RepositoryVisibility.PUBLIC
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.RoutePermission
import com.reposilite.token.api.RoutePermission.READ
import com.reposilite.token.api.RoutePermission.WRITE
import java.nio.file.Path

internal class RepositorySecurityProvider {

    fun canAccessRepository(accessToken: AccessToken?, repository: Repository): Boolean =
        when(repository.visibility) {
            PUBLIC -> true
            HIDDEN, PRIVATE -> accessToken?.canSee("/${repository.name}") ?: false
        }

    fun canAccessResource(accessToken: AccessToken?, repository: Repository, gav: Path): Boolean =
        when (repository.visibility) {
            PUBLIC -> true
            HIDDEN -> true
            PRIVATE -> hasPermissionTo(accessToken, repository, gav, READ)
        }

    fun canBrowseResource(accessToken: AccessToken?, repository: Repository, gav: Path): Boolean =
        when (repository.visibility) {
            PUBLIC -> true
            HIDDEN -> hasPermissionTo(accessToken, repository, gav, READ)
            PRIVATE -> hasPermissionTo(accessToken, repository, gav, READ)
        }

    fun canModifyResource(accessToken: AccessToken?, repository: Repository, gav: Path): Boolean =
        hasPermissionTo(accessToken, repository, gav, WRITE)

    private fun hasPermissionTo(accessToken: AccessToken?, repository: Repository, gav: Path, permission: RoutePermission): Boolean =
        accessToken?.hasPermissionTo("/" + repository.name + "/" + gav.toString().replace("\\", "/"), permission) ?: false

}