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
import org.panda_lang.reposilite.token.AccessTokenRepository
import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.api.Route

internal class SqlAccessTokenRepository : AccessTokenRepository {

    init {
        transaction {
            SchemaUtils.create(AccessTokenTable, RouteTable)
        }
    }

    override fun createAccessToken(accessToken: AccessToken) {
        transaction {
            AccessTokenTable.insert {
                it[alias] = accessToken.alias
                it[secret] = accessToken.secret
                it[permissions] = accessToken.permissions
            }
        }
    }

    override fun updateAccessToken(accessToken: AccessToken) {
        transaction {
            AccessTokenTable.update( { AccessTokenTable.id eq accessToken.id } ) {
                it[alias] = accessToken.alias
                it[secret] = accessToken.secret
                it[permissions] = accessToken.permissions
            }
        }
    }

    override fun createRoute(accessToken: AccessToken, route: Route) {
        transaction {
            RouteTable.insert {
                it[accessTokenId] = accessToken.id
                it[path] = route.path
                it[permissions] = route.permissions
            }
        }
    }

    override fun deleteAccessTokenByAlias(alias: String) {
        transaction {
            AccessTokenTable.deleteWhere { AccessTokenTable.alias eq alias }
        }
    }

    override fun deleteRoute(route: Route) {
        transaction {
            RouteTable.deleteWhere { RouteTable.id eq route.id }
        }
    }

    override fun findAccessTokenByAlias(alias: String): AccessToken? = transaction {
        AccessTokenTable
            .select { AccessTokenTable.alias eq alias }
            .map { toAccessToken(it) }
            .firstOrNull()
    }

    private fun findRoutesById(accessTokenId: Int): List<Route> = transaction {
        RouteTable
            .select { RouteTable.accessTokenId eq accessTokenId }
            .map { Route(it[RouteTable.id].value, it[RouteTable.path], it[RouteTable.permissions]) }
    }

    override fun findAll(): Collection<AccessToken> = transaction {
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
                result[AccessTokenTable.permissions],
                findRoutesById(accessTokenId)
            )
        }

    override fun countAccessTokens(): Long = transaction {
        AccessTokenTable.selectAll().count()
    }

}