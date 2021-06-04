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

package org.panda_lang.reposilite.token.infrastructure

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.panda_lang.reposilite.shared.sql.IdentifiableEntity
import org.panda_lang.reposilite.shared.sql.transactionUnit
import org.panda_lang.reposilite.token.PermissionRepository
import org.panda_lang.reposilite.token.api.Permission

internal class SqlPermissionRepository : PermissionRepository {

    init {
        transaction {
            SchemaUtils.create(PermissionsTable)
        }
    }

    override fun savePermission(entity: IdentifiableEntity, permission: Permission) =
        transactionUnit {
            PermissionsTable.insert {
                it[ownerId] = entity.id
                it[type] = permission.type
                it[name] = permission.name
            }
        }

    override fun findPermissionsById(entityId: Int, type: String, permissionSource: Collection<Permission>): Collection<Permission> =
        transaction {
            PermissionsTable.select { Op.build { PermissionsTable.ownerId eq entityId }.and { PermissionsTable.type eq type } }
                .map { "${it[PermissionsTable.type]}@${it[PermissionsTable.name]}" }
                .map { permissionSource.first { stored -> stored.toString() == it } }
                .toSet()
        }

    override fun deletePermissions(entityId: Int, type: String) =
        transactionUnit {
            PermissionsTable.deleteWhere { Op.build { PermissionsTable.ownerId eq entityId }.and { PermissionsTable.type eq type } }
        }

}