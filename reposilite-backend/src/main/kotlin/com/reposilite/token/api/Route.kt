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
import panda.std.Result

data class Route(
    val path: String,
    val permissions: Set<RoutePermission>
) {

    fun hasPermissionTo(toPath: String, routePermission: RoutePermission): Boolean =
        toPath.startsWith(path) && permissions.contains(routePermission)

}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class RoutePermission(val identifier: String, val shortcut: String) {

    READ("route:read", "r"),
    WRITE("route:write", "w");

    companion object {

        fun findRoutePermissionByIdentifier(identifier: String): Result<RoutePermission, String> =
            Result.ok(values().firstOrNull { it.identifier == identifier })
                ?: Result.error("Unknown permission identifier ($identifier) available options (${values().joinToString { it.identifier }})")

        fun findRoutePermissionByShortcut(shortcut: String): Result<RoutePermission, String> =
            Result.ok(values().firstOrNull { it.shortcut == shortcut })
                ?: Result.error("Unknown permission shortcut ($shortcut) available options (${values().joinToString { it.shortcut }})")

    }

}