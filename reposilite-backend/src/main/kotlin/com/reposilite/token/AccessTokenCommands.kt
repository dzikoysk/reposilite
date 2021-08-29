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

import com.reposilite.console.ReposiliteCommand
import com.reposilite.console.Status
import com.reposilite.console.Status.FAILED
import com.reposilite.console.Status.SUCCEEDED
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.CreateAccessTokenRequest
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "tokens", description = ["List all generated tokens"])
internal class TokensCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    override fun execute(output: MutableList<String>): Status {
        output.add("")
        output.add("Tokens (${accessTokenFacade.count()})")

        accessTokenFacade.getTokens().forEach {
            output.add("- ${it.name}:")

            it.routes.forEach { route ->
                output.add("  > ${route.path} : ${route.permissions}")
            }

            if (it.routes.isEmpty()) {
                output.add("  > ~ no routes ~")
            }
        }

        return SUCCEEDED
    }

}

@Command(name = "keygen", description = ["Generate a new access token"])
internal class KeygenCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["access token name"])
    private lateinit var name: String

    @Parameters(
        index = "1",
        paramLabel = "[<permissions>]",
        description = ["extra permissions: m - manager"],
        defaultValue = ""
    )
    private lateinit var permissions: String

    private val permissionMap = mapOf(
        'm' to AccessTokenPermission.MANAGER
    )

    override fun execute(output: MutableList<String>): Status {
        val token = accessTokenFacade.createAccessToken(CreateAccessTokenRequest(
            name,
            permissions = permissions.toCharArray()
                .map { permissionMap[it]!! }
                .toSet()
        ))
        output.add("Generated new access token for $name with '$permissions' permissions. Secret:")
        output.add(token.secret)
        return SUCCEEDED
    }
}

@Command(name = "chname", description = ["Change token name"])
internal class ChNameCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["name of token to update"])
    private lateinit var name: String

    @Parameters(index = "1", paramLabel = "<new name>", description = ["new token name"])
    private lateinit var updatedName: String

    override fun execute(output: MutableList<String>): Status =
        accessTokenFacade.getToken(name)
            ?.let {
                accessTokenFacade.updateToken(it.copy(name = updatedName))
                output.add("Token name has been changed from '$name' to '$updatedName'")
                SUCCEEDED
            }
            ?: run {
                output.add("Token '$name' not found")
                FAILED
            }

}

@Command(name = "chmod", description = ["Change token permissions"])
internal class ChModCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["name of token to update"])
    private lateinit var token: String

    @Parameters(index = "1", paramLabel = "<permissions>", description = ["new permissions"])
    private lateinit var permissions: String

    override fun execute(output: MutableList<String>): Status =
        accessTokenFacade.getToken(token)
            ?.let {
                // TOFIX somehow map user input to permissions
                // accessTokenFacade.updateToken(it.copy(permissions = permissions))
                output.add("Permissions have been changed from '${it.permissions}' to '$permissions'")
                SUCCEEDED
            }
            ?: run {
                output.add("Token '$token' not found")
                FAILED
            }

}

@Command(name = "revoke", description = ["Revoke token"])
internal class RevokeCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = ["name of token to revoke"])
    private lateinit var name: String

    override fun execute(output: MutableList<String>): Status {
        accessTokenFacade.deleteToken(name)
        output.add("Token for '$name' has been revoked")
        return SUCCEEDED
    }

}
