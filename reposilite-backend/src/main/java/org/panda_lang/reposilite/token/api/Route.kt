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

data class Route internal constructor(
    override val id: Int = UNINITIALIZED_ENTITY_ID,
    val path: String,
    val permissions: Collection<Permission>
) : IdentifiableEntity {

    companion object {
        const val PERMISSION_TYPE = "route"
        val READ = Permission(PERMISSION_TYPE, "read")
        val WRITE = Permission(PERMISSION_TYPE, "write")
        val PERMISSIONS = listOf(READ, WRITE)
    }

    fun addPermission(permission: Permission): Route =
        copy(permissions = permissions + permission)

    fun removePermission(permission: Permission): Route =
        copy(permissions = permissions - permission)

    fun hasPermissionTo(toPath: String, routePermission: Permission): Boolean =
        toPath.startsWith(path) && permissions.contains(routePermission)

}