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
package org.panda_lang.reposilite.token.api

import org.panda_lang.reposilite.shared.sql.IdentifiableEntity
import org.panda_lang.reposilite.shared.sql.UNINITIALIZED_ENTITY_ID
import org.panda_lang.reposilite.token.api.Route.Companion.READ

data class AccessToken internal constructor(
    override val id: Int = UNINITIALIZED_ENTITY_ID,
    val alias: String,
    val secret: String,
    val permissions: Collection<Permission> = emptyList(),
    val routes: Collection<Route> = emptyList()
) : IdentifiableEntity {

    companion object {
        const val PERMISSION_TYPE = "access_token"
        val MANAGER = Permission(PERMISSION_TYPE, "manager")
        val PERMISSIONS = listOf(MANAGER)
    }

    fun addPermission(permission: Permission): AccessToken =
        copy(permissions = permissions + permission)

    fun removePermission(permission: Permission): AccessToken =
        copy(permissions = permissions - permission)

    fun hasPermission(permission: Permission): Boolean =
        permissions.contains(permission)

    fun addRoute(route: Route): AccessToken =
        copy(routes = routes + route)

    fun removeRoute(route: Route): AccessToken =
        copy(routes = routes - route)

    fun hasPermissionTo(toPath: String, routePermission: Permission = READ): Boolean =
        routes.any { it.hasPermissionTo(toPath, routePermission) }

}