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
import com.reposilite.shared.transactionUnit
import com.reposilite.token.AccessTokenRepository
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.AccessTokenPermission.Companion.findAccessTokenPermissionByIdentifier
import com.reposilite.token.api.AccessTokenType
import com.reposilite.token.api.Route
import com.reposilite.token.api.RoutePermission.Companion.findRoutePermissionByIdentifier
import net.dzikoysk.exposed.shared.UNINITIALIZED_ENTITY_ID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

internal class SqlAccessTokenRepository : AccessTokenRepository {

    init {
        transaction {
            SchemaUtils.create(AccessTokenTable, PermissionToAccessTokenTable, PermissionToRouteTable)
        }
    }

    override fun saveAccessToken(accessToken: AccessToken) {
        transaction {
            // Make sure to remove an existing token with the same name
            var id = getIdByName(accessToken.name)
            AccessTokenTable.deleteWhere { AccessTokenTable.id eq id }

            AccessTokenTable.insert {
                if (accessToken.id != UNINITIALIZED_ENTITY_ID) {
                    it[this.id] = accessToken.id
                }

                it[this.name] = accessToken.name
                it[this.secret] = accessToken.secret
            }

            // Fetch new id
            id = getIdByName(accessToken.name)

            accessToken.permissions.forEach { permission ->
                PermissionToAccessTokenTable.insert {
                    it[this.accessTokenId] = id
                    it[this.permission] = permission.identifier
                }
            }

            accessToken.routes.forEach { route ->
                route.permissions.forEach { permission ->
                    PermissionToRouteTable.insert {
                        it[this.accessTokenId] = id
                        it[this.permission] = permission.identifier
                        it[this.route] = route.path
                    }
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

    override fun deleteAccessToken(accessToken: AccessToken) =
        transactionUnit { AccessTokenTable.deleteWhere { AccessTokenTable.id eq accessToken.id } }

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

    override fun findAccessTokenByName(name: String): AccessToken? =
        transaction { AccessTokenTable.select { AccessTokenTable.name eq name }.firstAndMap { toAccessToken(it) } }

    override fun findAll(): Collection<AccessToken> =
        transaction { AccessTokenTable.selectAll().map { toAccessToken(it) } }

    override fun countAccessTokens(): Long =
        transaction { AccessTokenTable.selectAll().count() }

}