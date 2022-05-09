/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.token

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import panda.std.Result
import panda.std.asSuccess

data class Route(
    val path: String,
    val permission: RoutePermission
) {

    fun hasPermissionTo(toPath: String, requiredPermission: RoutePermission): Boolean =
        permission == requiredPermission && toPath.startsWith(path, ignoreCase = true)

}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class RoutePermission(val identifier: String, val shortcut: String) {

    READ("route:read", "r"),
    WRITE("route:write", "w");

    companion object {

        fun findRoutePermissionByIdentifier(identifier: String): Result<RoutePermission, String> =
            values()
                .firstOrNull { it.identifier == identifier }
                ?.asSuccess()
                ?: Result.error("Unknown permission identifier ($identifier) available options (${values().joinToString { it.identifier }})")

        fun findRoutePermissionByShortcut(shortcut: String): Result<RoutePermission, String> =
            values()
                .firstOrNull { it.shortcut == shortcut }
                ?.asSuccess()
                ?: Result.error("Unknown permission shortcut ($shortcut) available options (${values().joinToString { it.shortcut }})")

        @JsonCreator
        @JvmStatic
        fun fromObject(data: Map<String, String>): RoutePermission =
            findRoutePermissionByIdentifier(data["identifier"]!!).orElseThrow { IllegalArgumentException(it) }

    }

}