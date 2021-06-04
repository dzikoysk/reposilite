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

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.panda_lang.reposilite.shared.sql.transactionUnit
import org.panda_lang.reposilite.token.PermissionRepository
import org.panda_lang.reposilite.token.RouteRepository
import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.api.Route

internal class SqlRouteRepository(private val permissionRepository: PermissionRepository) : RouteRepository {

    init {
        transaction {
            SchemaUtils.create(RouteTable)
        }
    }

    override fun saveRoute(accessToken: AccessToken, route: Route) =
        transactionUnit {
            RouteTable.insert {
                it[accessTokenId] = accessToken.id
                it[path] = route.path
            }
        }

    override fun deleteRoute(route: Route) =
        transactionUnit {
            RouteTable.deleteWhere { RouteTable.id eq route.id }
        }

    override fun findRoutesById(accessTokenId: Int): Collection<Route> =
        transaction {
            RouteTable.select { RouteTable.accessTokenId eq accessTokenId }
                .map {
                    it[RouteTable.id].value.let { routeId ->
                        Route(
                            routeId,
                            it[RouteTable.path],
                            permissionRepository.findPermissionsById(routeId, Route.PERMISSION_TYPE, Route.PERMISSIONS)
                        )
                    }
                }
        }

}