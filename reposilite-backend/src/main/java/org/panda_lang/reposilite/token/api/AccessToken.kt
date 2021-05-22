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

import io.javalin.plugin.openapi.annotations.HttpMethod
import io.javalin.plugin.openapi.annotations.HttpMethod.GET
import io.javalin.plugin.openapi.annotations.HttpMethod.HEAD
import io.javalin.plugin.openapi.annotations.HttpMethod.POST
import io.javalin.plugin.openapi.annotations.HttpMethod.PUT
import org.panda_lang.reposilite.token.api.RoutePermission.READ
import java.io.Serializable

const val UNINITIALIZED_ENTITY_ID = -1

data class AccessToken internal constructor(
    val id: Int = UNINITIALIZED_ENTITY_ID,
    val alias: String,
    val secret: String,
    val permissions: String,
    val routes: List<Route> = emptyList()
) : Serializable {

    fun hasPermission(permission: AccessTokenPermission) =
        permissions.contains(permission.symbol)

    fun hasPermissionTo(toPath: String, routePermission: RoutePermission = READ) =
        routes.any { it.hasPermissionTo(toPath, routePermission) }

}

enum class AccessTokenPermission(
    val symbol: Char,
    val isDefault: Boolean,
) {

    MANAGER('m', false);

    companion object {

        fun ofSymbols(permissions: String): Set<AccessTokenPermission> =
            values().filter { permissions.contains(it.symbol) }.toSet()

        fun getDefaultPermissions(): Set<AccessTokenPermission> =
            values().filter { it.isDefault }.toSet()

    }

}

data class Route internal constructor(
    val id: Int = UNINITIALIZED_ENTITY_ID,
    val path: String,
    val permissions: String
) {

    fun hasPermissionTo(toPath: String, routePermission: RoutePermission = READ) =
        toPath.startsWith(path) && permissions.contains(routePermission.symbol)

}

enum class RoutePermission(
    val symbol: String,
    val isDefault: Boolean,
    val methods: Array<HttpMethod>
) {

    WRITE("w", false, arrayOf(PUT, POST)),
    READ("r", true, arrayOf(HEAD, GET));

    companion object {

        fun getDefaultPermissions(): Set<RoutePermission> =
            values().filter { it.isDefault }.toSet()

        fun findByMethod(method: HttpMethod): RoutePermission? =
            values().find { it.methods.contains(method) }

    }

}