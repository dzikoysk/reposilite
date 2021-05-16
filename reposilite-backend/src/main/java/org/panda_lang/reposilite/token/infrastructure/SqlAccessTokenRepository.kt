package org.panda_lang.reposilite.token.infrastructure

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.panda_lang.reposilite.token.AccessTokenRepository
import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.api.Route

class SqlAccessTokenRepository : AccessTokenRepository {

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