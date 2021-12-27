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

package com.reposilite.token

import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.api.ReposiliteCommand
import com.reposilite.token.api.Route
import com.reposilite.token.api.RoutePermission
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "route-add", description = ["Add new route to access token"])
internal class RouteAdd(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<access_token>", description = ["Name of access token to modify"])
    private lateinit var name: String

    @Parameters(index = "1", paramLabel = "<path>", description = ["Path to the route, e.g. /releases/com/reposilite"])
    private lateinit var route: String

    @Parameters(index = "2", paramLabel = "<permissions>", description = [
        "Route permissions, e.g. rw. Available permissions:",
        "r - allows token to read resources under the associated path (route:read)",
        "w - allows token to write (deploy) resources under the associated path (route:write)"
    ])
    private lateinit var permissions: String

    override fun execute(context: CommandContext) {
        accessTokenFacade.getToken(name)?.also {
            if (it.routes.any { entry -> entry.path == route }) {
                context.status = FAILED
                context.append("Token $name already has route with path $route")
                return
            }

            val mappedPermissions = mapPermissions() ?: let {
                context.status = FAILED
                context.append("Unknown permission shortcuts (${permissions.toCharArray().joinToString()})")
                context.append("Available options (${RoutePermission.values().joinToString { perm -> perm.shortcut }})")
                return
            }

            val route = Route(route, mappedPermissions)
            val updatedToken = accessTokenFacade.updateToken(it.withRoute(route))
            context.append("Route $route has been added to token ${updatedToken.name}")
        }
        ?: run {
            context.status = FAILED
            context.append("Token $name not found")
        }
    }

    private fun mapPermissions(): Set<RoutePermission>? =
        permissions.toCharArray()
            .map { RoutePermission.findRoutePermissionByShortcut(it.toString()).orNull() }
            .filterNotNull()
            .toSet()
            .takeIf { it.isNotEmpty() }
}

@Command(name = "route-remove", description = ["Remove route from access token"])
internal class RouteRemove(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<access_token>", description = ["Name of access token to modify"])
    private lateinit var name: String

    @Parameters(index = "1", paramLabel = "<path>", description = ["Path of route to remove"])
    private lateinit var path: String

    override fun execute(context: CommandContext) {
        accessTokenFacade.getToken(name)?.also {
            val updatedToken = it.copy(routes = it.routes
                .filter { route -> route.path != path }
                .toSet()
            )
            accessTokenFacade.updateToken(updatedToken)
            context.append("Token $name has been updated, new routes: ${updatedToken.routes}")
        }
        ?: run {
            context.status = FAILED
            context.append("Token $name not found")
        }
    }

}
