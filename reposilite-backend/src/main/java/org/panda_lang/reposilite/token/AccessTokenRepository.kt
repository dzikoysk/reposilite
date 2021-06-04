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

package org.panda_lang.reposilite.token

import org.panda_lang.reposilite.shared.sql.IdentifiableEntity
import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.api.Permission
import org.panda_lang.reposilite.token.api.Route

internal interface AccessTokenRepository {

    fun saveAccessToken(accessToken: AccessToken)

    fun deleteAccessTokenByAlias(alias: String)

    fun findAccessTokenByAlias(alias: String): AccessToken?

    fun findAll(): Collection<AccessToken>

    fun countAccessTokens(): Long

}

internal interface RouteRepository {

    fun saveRoute(accessToken: AccessToken, route: Route)

    fun findRoutesById(accessTokenId: Int): Collection<Route>

    fun deleteRoute(route: Route)

}

internal interface PermissionRepository {

    fun savePermission(entity: IdentifiableEntity, permission: Permission)

    fun findPermissionsById(entityId: Int, type: String, permissionSource: Collection<Permission>): Collection<Permission>

    fun deletePermissions(entityId: Int, type: String)

}