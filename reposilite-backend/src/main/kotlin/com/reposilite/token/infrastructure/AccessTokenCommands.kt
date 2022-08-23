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

import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.api.ReposiliteCommand
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.api.CreateAccessTokenRequest
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

@Command(name = "tokens", description = ["List all generated tokens"])
internal class TokensCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    override fun execute(context: CommandContext) {
        context.append("Tokens (${accessTokenFacade.count()})")

        accessTokenFacade.getAccessTokens().forEach { token ->
            context.append("- ${token.name}: ${accessTokenFacade.getPermissions(token.identifier)}")
            val routes = accessTokenFacade.getRoutes(token.identifier)

            routes.groupBy { it.path }
                .forEach { (route, permissions) ->
                    context.append("  > $route : ${permissions.map { it.permission } }")
                }

            if (routes.isEmpty()) {
                context.append("  > ~ no routes ~")
            }
        }
    }

}

@Command(name = "token-generate", description = ["Generate a new access token"])
internal class KeygenCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Option(names = ["--secret", "-s"], description = ["Override generated token with custom secret"], required = false)
    var secret: String? = null

    @Parameters(index = "0", paramLabel = "<name>", description = ["Access token name"])
    private lateinit var name: String

    @Parameters(index = "1", paramLabel = "[<permissions>]", defaultValue = "", description = [
        "Access token permissions, e.g. m, optional. Available permissions",
        "m - marks token as management token and grants access to all features and paths by default (access_token:manager)"
    ])
    private lateinit var permissions: String

    override fun execute(context: CommandContext) {
        val mappedPermissions = mapPermissions(context, permissions) ?: return
        val response = accessTokenFacade.createAccessToken(CreateAccessTokenRequest(PERSISTENT, name, secret = secret))

        mappedPermissions.forEach {
            accessTokenFacade.addPermission(response.accessToken.identifier, it)
        }

        context.append("Generated new access token for $name with '$permissions' permissions. Secret:")
        context.append(response.secret)
    }

}

@Command(name = "token-modify", description = ["Change token permissions"])
internal class ChModCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["Name of token to update"])
    private lateinit var name: String

    @Parameters(index = "1", paramLabel = "<permissions>", description = ["New permissions"])
    private lateinit var permissions: String

    override fun execute(context: CommandContext) {
        val mappedPermissions = mapPermissions(context, permissions) ?: return

        accessTokenFacade.getAccessToken(name)
            ?.let { token ->
                AccessTokenPermission.values().forEach { accessTokenFacade.deletePermission(token.identifier, it) }
                mappedPermissions.forEach { accessTokenFacade.addPermission(token.identifier, it) }
                context.append("Permissions have been changed from to '$permissions'")
            }
            ?: run {
                context.status = FAILED
                context.append("Token '$name' not found")
            }
    }

}

private fun mapPermissions(context: CommandContext, permissions: String): Set<AccessTokenPermission>? {
    val mappedPermissions = permissions.toCharArray()
        .map { AccessTokenPermission.findAccessTokenPermissionByShortcut(it.toString()) }
        .takeIf { it.none { element -> element == null } }
        ?.filterNotNull()
        ?.toSet()

    if (mappedPermissions == null) {
        context.status = FAILED
        context.append("Unknown permissions '$permissions'. Type 'help token-generate' to display supported permissions")
        return null
    }

    return mappedPermissions
}

@Command(name = "token-rename", description = ["Change token name"])
internal class ChNameCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["Name of token to update"])
    private lateinit var name: String

    @Parameters(index = "1", paramLabel = "<new name>", description = ["New token name"])
    private lateinit var updatedName: String

    override fun execute(context: CommandContext) {
        accessTokenFacade.getAccessToken(name)
            ?.let {
                accessTokenFacade.updateToken(it.copy(name = updatedName))
                context.append("Token name has been changed from '$name' to '$updatedName'")
            }
            ?: run {
                context.status = FAILED
                context.append("Token '$name' not found")
            }
    }

}

@Command(name = "token-revoke", description = ["Revoke token"])
internal class RevokeCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["Name of token to revoke"])
    private lateinit var name: String

    override fun execute(context: CommandContext) {
        accessTokenFacade.getAccessToken(name)?.also { accessTokenFacade.deleteToken(it.identifier) }
        context.append("Token for '$name' has been revoked")
    }

}

@Command(name = "token-regenerate", description = ["Regenerate token"])
internal class RegenerateCommand(private val accessTokenFacade: AccessTokenFacade): ReposiliteCommand {
    @Parameters(index = "0", paramLabel = "<name>", description = ["Name of token to revoke"])
    private lateinit var name: String

    @Option(names = ["--secret", "-s"], description = ["Override generated token with custom secret"], required = false)
    var secret: String? = null

    override fun execute(context: CommandContext) {
        accessTokenFacade.getAccessToken(name)
            ?.also {
                accessTokenFacade.regenerateAccessToken(it, secret)
                    .consume(
                        { context.append("New token for '$name': $it") },
                        {
                            context.status = FAILED
                            context.append("Token '$name' not found")
                        }
                    )
            }
            ?: run {
                context.status = FAILED
                context.append("Token '$name' not found")
            }
    }
}