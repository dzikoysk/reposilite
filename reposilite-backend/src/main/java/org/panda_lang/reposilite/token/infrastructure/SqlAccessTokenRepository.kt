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

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.panda_lang.reposilite.shared.sql.UNINITIALIZED_ENTITY_ID
import org.panda_lang.reposilite.token.AccessTokenRepository
import org.panda_lang.reposilite.token.PermissionRepository
import org.panda_lang.reposilite.token.RouteRepository
import org.panda_lang.reposilite.token.api.AccessToken

internal class SqlAccessTokenRepository(
    private val routeRepository: RouteRepository,
    private val permissionRepository: PermissionRepository
) : AccessTokenRepository {

    init {
        transaction {
            SchemaUtils.create(AccessTokenTable)
        }
    }

    override fun saveAccessToken(accessToken: AccessToken) {
        transaction {
            if (accessToken.id == UNINITIALIZED_ENTITY_ID) {
                AccessTokenTable.insert {
                    it[alias] = accessToken.alias
                    it[secret] = accessToken.secret
                }
            }
            else {
                AccessTokenTable.update( { AccessTokenTable.id eq accessToken.id } ) {
                    it[alias] = accessToken.alias
                    it[secret] = accessToken.secret
                }
            }
        }
    }

    override fun deleteAccessTokenByAlias(alias: String) {
        findAccessTokenByAlias(alias)?.also {
            transaction {
                AccessTokenTable.deleteWhere { AccessTokenTable.id eq it.id }
                permissionRepository.deletePermissions(it.id, AccessToken.PERMISSION_TYPE)
            }
        }
    }

    override fun findAccessTokenByAlias(alias: String): AccessToken? =
        transaction {
            AccessTokenTable
                .select { AccessTokenTable.alias eq alias }
                .map { toAccessToken(it) }
                .firstOrNull()
        }

    override fun findAll(): Collection<AccessToken> =
        transaction {
            AccessTokenTable
                .selectAll()
                .map { toAccessToken(it) }
        }

    private fun toAccessToken(result: ResultRow): AccessToken =
        result[AccessTokenTable.id].value.let { accessTokenId ->
            AccessToken(
                accessTokenId,
                result[AccessTokenTable.alias],
                result[AccessTokenTable.secret],
                permissionRepository.findPermissionsById(accessTokenId, AccessToken.PERMISSION_TYPE, AccessToken.PERMISSIONS),
                routeRepository.findRoutesById(accessTokenId)
            )
        }

    override fun countAccessTokens(): Long =
        transaction {
            AccessTokenTable.selectAll().count()
        }

}