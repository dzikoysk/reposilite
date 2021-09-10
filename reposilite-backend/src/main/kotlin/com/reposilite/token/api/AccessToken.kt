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
package com.reposilite.token.api

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.reposilite.token.api.AccessTokenPermission.MANAGER
import com.reposilite.token.api.AccessTokenType.PERSISTENT
import io.javalin.openapi.OpenApiIgnore
import net.dzikoysk.exposed.shared.IdentifiableEntity
import net.dzikoysk.exposed.shared.UNINITIALIZED_ENTITY_ID
import java.time.LocalDate

data class AccessToken internal constructor(
    override val id: Int = UNINITIALIZED_ENTITY_ID,
    val type: AccessTokenType = PERSISTENT,
    val name: String,
    @Transient @JsonIgnore @get:OpenApiIgnore
    val secret: String,
    val createdAt: LocalDate = LocalDate.now(),
    val description: String = "",
    val permissions: Set<AccessTokenPermission> = emptySet(),
    val routes: Set<Route> = emptySet()
) : IdentifiableEntity {

    fun withRoute(route: Route): AccessToken =
        copy(routes = routes.toMutableSet().also { it.add(route) })

    fun hasPermission(permission: AccessTokenPermission): Boolean =
        permissions.contains(permission)

    private fun isManager(): Boolean =
        hasPermission(MANAGER)

    fun hasPermissionTo(toPath: String, routePermission: RoutePermission): Boolean =
        isManager() || routes.any { it.hasPermissionTo(toPath, routePermission) }

    fun canSee(routeFragment: String): Boolean =
        isManager() || routes.any { it.path.startsWith("$routeFragment/", ignoreCase = true) }

}

enum class AccessTokenType {
    PERSISTENT,
    TEMPORARY
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class AccessTokenPermission(val identifier: String, val shortcut: String) {

    MANAGER("access-token:manager", "m");

    companion object {

        fun findAccessTokenPermissionByIdentifier(identifier: String): AccessTokenPermission =
            values().first { it.identifier == identifier }

        fun findAccessTokenPermissionByShortcut(shortcut: String): AccessTokenPermission =
            values().first { it.shortcut == shortcut }

    }

}


