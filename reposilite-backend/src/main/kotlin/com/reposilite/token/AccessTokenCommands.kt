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
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.CreateAccessTokenRequest
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "tokens", description = ["List all generated tokens"])
internal class TokensCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    override fun execute(context: CommandContext) {
        context.append("Tokens (${accessTokenFacade.count()})")

        accessTokenFacade.getTokens().forEach {
            context.append("- ${it.name}:")

            it.routes.forEach { route ->
                context.append("  > ${route.path} : ${route.permissions}")
            }

            if (it.routes.isEmpty()) {
                context.append("  > ~ no routes ~")
            }
        }
    }

}

@Command(name = "keygen", description = ["Generate a new access token"])
internal class KeygenCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["access token name"])
    private lateinit var name: String

    @Parameters(index = "1", paramLabel = "[<permissions>]", description = ["extra permissions: m - manager"], defaultValue = "")
    private lateinit var permissions: String

    override fun execute(context: CommandContext) {
        val token = accessTokenFacade.createAccessToken(CreateAccessTokenRequest(
            name,
            permissions = permissions.toCharArray()
                .map { AccessTokenPermission.findAccessTokenPermissionByShortcut(it.toString()) }
                .toSet()
        ))

        context.append("Generated new access token for $name with '$permissions' permissions. Secret:")
        context.append(token.secret)
    }
}

@Command(name = "chname", description = ["Change token name"])
internal class ChNameCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["name of token to update"])
    private lateinit var name: String

    @Parameters(index = "1", paramLabel = "<new name>", description = ["new token name"])
    private lateinit var updatedName: String

    override fun execute(context: CommandContext) {
        accessTokenFacade.getToken(name)
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

@Command(name = "chmod", description = ["Change token permissions"])
internal class ChModCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["name of token to update"])
    private lateinit var token: String

    @Parameters(index = "1", paramLabel = "<permissions>", description = ["new permissions"])
    private lateinit var permissions: String

    override fun execute(context: CommandContext) {
        accessTokenFacade.getToken(token)
            ?.let {
                accessTokenFacade.updateToken(it.copy(
                    permissions = permissions.toCharArray()
                        .map { AccessTokenPermission.findAccessTokenPermissionByShortcut(it.toString()) }
                        .toSet()
                ))
                context.append("Permissions have been changed from '${it.permissions}' to '$permissions'")
            }
            ?: run {
                context.status = FAILED
                context.append("Token '$token' not found")
            }
    }

}

@Command(name = "revoke", description = ["Revoke token"])
internal class RevokeCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["name of token to revoke"])
    private lateinit var name: String

    override fun execute(context: CommandContext) {
        accessTokenFacade.deleteToken(name)
        context.append("Token for '$name' has been revoked")
    }

}
