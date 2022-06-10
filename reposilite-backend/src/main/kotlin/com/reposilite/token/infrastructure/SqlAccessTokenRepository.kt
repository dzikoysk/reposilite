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

package com.reposilite.token.infrastructure

import com.reposilite.token.AccessToken
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.AccessTokenPermission.Companion.findAccessTokenPermissionByIdentifier
import com.reposilite.token.AccessTokenRepository
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission.Companion.findRoutePermissionByIdentifier
import com.reposilite.token.application.AccessTokenPlugin.Companion.MAX_ROUTE_LENGTH
import com.reposilite.token.application.AccessTokenPlugin.Companion.MAX_TOKEN_NAME
import net.dzikoysk.exposed.shared.UNINITIALIZED_ENTITY_ID
import net.dzikoysk.exposed.upsert.withIndex
import net.dzikoysk.exposed.upsert.withUnique
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import panda.std.firstAndMap
import java.util.UUID

object AccessTokenTable : IntIdTable("access_token") {
    val name = varchar("name", MAX_TOKEN_NAME).uniqueIndex("uq_name")
    val secret = varchar("secret", 512)
    val createdAt = date("createdAt")
    val description = text("description")
}

object PermissionToAccessTokenTable : Table("permission_access_token") {
    val accessTokenId = reference("access_token_id", AccessTokenTable.id, onDelete = CASCADE, onUpdate = CASCADE)
    val permission = varchar("permission", 48)

    init {
        withIndex("index_access_token_id", columns = arrayOf(accessTokenId))
        withUnique("unique_access_token_id_permission", accessTokenId, permission)
    }
}

object PermissionToRouteTable : Table("permission_route") {
    val accessTokenId = reference("access_token_id", AccessTokenTable.id, onDelete = CASCADE, onUpdate = CASCADE)
    val routeId = uuid("route_id")
    val route = varchar("route", MAX_ROUTE_LENGTH)
    val permission = varchar("permission", 48)

    init {
        withIndex("index_access_token_id_route_id", columns = arrayOf(accessTokenId, routeId))
        withUnique("unique_access_token_id_route_id_permission", accessTokenId, routeId, permission)
    }
}

internal class SqlAccessTokenRepository(private val database: Database) : AccessTokenRepository {

    init {
        transaction(database) {
            SchemaUtils.create(AccessTokenTable, PermissionToAccessTokenTable, PermissionToRouteTable)
            SchemaUtils.createMissingTablesAndColumns(AccessTokenTable, PermissionToAccessTokenTable, PermissionToRouteTable)
        }
    }

    override fun saveAccessToken(accessToken: AccessToken): AccessToken =
        transaction(database) {
            when(getIdByName(accessToken.name)) {
                UNINITIALIZED_ENTITY_ID -> createAccessToken(accessToken)
                else -> updateAccessToken(accessToken)
            }
        }

    private fun createAccessToken(accessToken: AccessToken): AccessToken {
        AccessTokenTable.insert {
            it[this.name] = accessToken.name
            it[this.secret] = accessToken.encryptedSecret
            it[this.createdAt] = accessToken.createdAt
            it[this.description] = accessToken.description
        }
        return accessToken.copy(identifier = AccessTokenIdentifier(PERSISTENT, getIdByName(accessToken.name)))
    }

    private fun updateAccessToken(accessToken: AccessToken): AccessToken {
        AccessTokenTable.update({ AccessTokenTable.id eq accessToken.identifier.value }, body = {
            it[this.name] = accessToken.name
            it[this.secret] = accessToken.encryptedSecret
            it[this.createdAt] = accessToken.createdAt
            it[this.description] = accessToken.description
        })
        return accessToken
    }

    override fun addPermission(id: AccessTokenIdentifier, permission: AccessTokenPermission) {
        transaction(database) {
            PermissionToAccessTokenTable.insert {
                it[this.accessTokenId] = id.value
                it[this.permission] = permission.identifier
            }
        }
    }

    override fun deletePermission(id: AccessTokenIdentifier, permission: AccessTokenPermission) {
        transaction(database) {
            PermissionToRouteTable.deleteWhere { PermissionToRouteTable.accessTokenId eq id.value }
        }
    }

    override fun addRoute(id: AccessTokenIdentifier, route: Route) {
        transaction(database) {
            PermissionToRouteTable.insert {
                it[this.accessTokenId] = id.value
                it[this.routeId] = UUID.nameUUIDFromBytes(route.path.toByteArray())
                it[this.route] = route.path
                it[this.permission] = route.permission.identifier
            }
        }
    }

    override fun deleteRoute(id: AccessTokenIdentifier, route: Route) {
        transaction(database) {
            PermissionToRouteTable.deleteWhere { PermissionToRouteTable.accessTokenId eq id.value }
        }
    }

    private fun getIdByName(name: String): Int =
        AccessTokenTable.select { AccessTokenTable.name eq name }
            .map { it[AccessTokenTable.id] }
            .firstOrNull()
            ?.value
            ?: UNINITIALIZED_ENTITY_ID

    override fun deleteAccessToken(id: AccessTokenIdentifier) {
        transaction(database) {
            AccessTokenTable.deleteWhere { AccessTokenTable.id eq id.value }
        }
    }

    override fun findAccessTokenPermissionsById(id: AccessTokenIdentifier): Set<AccessTokenPermission> =
        transaction(database) {
            PermissionToAccessTokenTable.select { PermissionToAccessTokenTable.accessTokenId eq id.value }
                .map { findAccessTokenPermissionByIdentifier(it[PermissionToAccessTokenTable.permission])!! }
                .toSet()
        }

    override fun findAccessTokenRoutesById(id: AccessTokenIdentifier): Set<Route> =
        transaction(database) {
            PermissionToRouteTable.select { PermissionToRouteTable.accessTokenId eq id.value }
                .map { Route(it[PermissionToRouteTable.route], findRoutePermissionByIdentifier(it[PermissionToRouteTable.permission]).get()) }
                .toSet()
        }

    private fun toAccessToken(result: ResultRow): AccessToken =
        AccessToken(
            AccessTokenIdentifier(PERSISTENT, result[AccessTokenTable.id].value),
            result[AccessTokenTable.name],
            result[AccessTokenTable.secret],
            result[AccessTokenTable.createdAt],
            result[AccessTokenTable.description]
        )

    override fun findAccessTokenByName(name: String): AccessToken? =
        transaction(database) {
            AccessTokenTable.select { AccessTokenTable.name eq name }.firstAndMap { toAccessToken(it) }
        }

    override fun findAccessTokenById(id: AccessTokenIdentifier): AccessToken? =
        transaction(database) {
            AccessTokenTable.select { AccessTokenTable.id eq id.value }.firstAndMap { toAccessToken(it) }
        }

    override fun findAll(): Collection<AccessToken> =
        transaction(database) {
            AccessTokenTable.selectAll().map { toAccessToken(it) }
        }

    override fun countAccessTokens(): Long =
        transaction(database) {
            AccessTokenTable.selectAll().count()
        }

}