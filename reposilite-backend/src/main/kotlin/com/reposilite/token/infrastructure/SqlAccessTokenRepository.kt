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

package com.reposilite.token.infrastructure

import com.reposilite.shared.firstAndMap
import com.reposilite.shared.launchTransaction
import com.reposilite.token.AccessTokenRepository
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.AccessTokenPermission.Companion.findAccessTokenPermissionByIdentifier
import com.reposilite.token.api.AccessTokenType
import com.reposilite.token.api.Route
import com.reposilite.token.api.RoutePermission.Companion.findRoutePermissionByIdentifier
import kotlinx.coroutines.CoroutineDispatcher
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
import java.util.UUID

const val MAX_ROUTE_LENGTH = 1024

object AccessTokenTable : IntIdTable("access_token") {
    val name = varchar("name", 255).uniqueIndex("uq_name")
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

internal class SqlAccessTokenRepository(private val dispatcher: CoroutineDispatcher?, private val database: Database) : AccessTokenRepository {

    init {
        transaction(database) {
            SchemaUtils.create(AccessTokenTable, PermissionToAccessTokenTable, PermissionToRouteTable)
            SchemaUtils.createMissingTablesAndColumns(AccessTokenTable, PermissionToAccessTokenTable, PermissionToRouteTable)
        }
    }

    override suspend fun saveAccessToken(accessToken: AccessToken): AccessToken =
        launchTransaction(dispatcher, database) {
            when(getIdByName(accessToken.name)) {
                UNINITIALIZED_ENTITY_ID -> createAccessToken(accessToken)
                else -> updateAccessToken(accessToken)
            }
        }

    private fun createAccessToken(accessToken: AccessToken): AccessToken {
        AccessTokenTable.insert {
            it[this.name] = accessToken.name
            it[this.secret] = accessToken.secret
            it[this.createdAt] = accessToken.createdAt
            it[this.description] = accessToken.description
        }

        val createdAccessToken = accessToken.copy(id = getIdByName(accessToken.name))
        createPermissions(createdAccessToken)
        createRoutes(createdAccessToken)

        return createdAccessToken
    }

    private fun updateAccessToken(accessToken: AccessToken): AccessToken {
        AccessTokenTable.update({ AccessTokenTable.id eq accessToken.id }, body = {
            it[this.name] = accessToken.name
            it[this.secret] = accessToken.secret
            it[this.createdAt] = accessToken.createdAt
            it[this.description] = accessToken.description
        })

        PermissionToAccessTokenTable.deleteWhere { PermissionToAccessTokenTable.accessTokenId eq accessToken.id }
        createPermissions(accessToken)

        PermissionToRouteTable.deleteWhere { PermissionToRouteTable.accessTokenId eq accessToken.id }
        createRoutes(accessToken)

        return accessToken
    }

    private fun createPermissions(accessToken: AccessToken) {
        accessToken.permissions.forEach { permission ->
            PermissionToAccessTokenTable.insert {
                it[this.accessTokenId] = accessToken.id
                it[this.permission] = permission.identifier
            }
        }
    }

    private fun createRoutes(accessToken: AccessToken) {
        accessToken.routes.forEach { route ->
            route.permissions.forEach { permission ->
                PermissionToRouteTable.insert {
                    it[this.accessTokenId] = accessToken.id
                    it[this.routeId] = UUID.nameUUIDFromBytes(route.path.toByteArray())
                    it[this.route] = route.path
                    it[this.permission] = permission.identifier
                }
            }
        }
    }

    private fun getIdByName(name: String): Int =
        AccessTokenTable.select { AccessTokenTable.name eq name }
            .map { it[AccessTokenTable.id] }
            .firstOrNull()
            ?.value
            ?: UNINITIALIZED_ENTITY_ID

    override suspend fun deleteAccessToken(accessToken: AccessToken) {
        launchTransaction(dispatcher, database) {
            AccessTokenTable.deleteWhere { AccessTokenTable.id eq accessToken.id }
        }
    }

    private fun findAccessTokenPermissionsById(id: Int): Set<AccessTokenPermission> =
        PermissionToAccessTokenTable.select { PermissionToAccessTokenTable.accessTokenId eq id }
            .map { findAccessTokenPermissionByIdentifier(it[PermissionToAccessTokenTable.permission]) }
            .toSet()

    private fun findRoutesById(id: Int): Set<Route> =
        PermissionToRouteTable.select { PermissionToRouteTable.accessTokenId eq id }
            .map { Pair(it[PermissionToRouteTable.route], it[PermissionToRouteTable.permission]) }
            .groupBy { it.first }
            .map { (route, permissions) ->
                Route(route, permissions.map { findRoutePermissionByIdentifier(it.second) }.toSet())
            }
            .toSet()

    private fun toAccessToken(result: ResultRow): AccessToken =
        result[AccessTokenTable.id].value.let { accessTokenId ->
            AccessToken(
                accessTokenId,
                AccessTokenType.PERSISTENT,
                result[AccessTokenTable.name],
                result[AccessTokenTable.secret],
                result[AccessTokenTable.createdAt],
                result[AccessTokenTable.description],
                findAccessTokenPermissionsById(accessTokenId),
                findRoutesById(accessTokenId)
            )
        }

    override suspend fun findAccessTokenByName(name: String): AccessToken? =
        launchTransaction(dispatcher, database) {
            AccessTokenTable.select { AccessTokenTable.name eq name }.firstAndMap { toAccessToken(it) }
        }

    override suspend fun findAll(): Collection<AccessToken> =
        launchTransaction(dispatcher, database) {
            AccessTokenTable.selectAll().map { toAccessToken(it) }
        }

    override suspend fun countAccessTokens(): Long =
        launchTransaction(dispatcher, database) {
            AccessTokenTable.selectAll().count()
        }

}