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

package org.panda_lang.reposilite.auth

import org.panda_lang.reposilite.console.ReposiliteCommand
import org.panda_lang.reposilite.token.AccessTokenFacade
import org.panda_lang.reposilite.token.api.RoutePermission
import org.panda_lang.utilities.commons.StringUtils
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.IOException

@Command(name = "tokens", description = ["List all generated tokens"])
internal class TokensCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    override fun execute(response: MutableList<String>): Boolean {
        response.add("Tokens (${accessTokenFacade.count()})")

        accessTokenFacade.getTokens().forEach {
            response.add(it.alias + ":'")

            it.routes.forEach { route ->
                response.add("  ${route.path} : ${route.permissions}")
            }
        }

        return true
    }

}

@Command(name = "keygen", description = ["Generate a new access token"])
internal class KeygenCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<alias>", description = ["access token alias"])
    private lateinit var alias: String

    @Parameters(
        index = "2",
        paramLabel = "[<permissions>]",
        description = ["extra permissions: m - manager, w - write r - read (optional)"],
        defaultValue = ""
    )
    private lateinit var permissions: String

    override fun execute(response: MutableList<String>): Boolean {
        var processedPath = path

        // Support simplified artifact qualifier (x.y.z instead of /x/y/z)
        // ~ https://github.com/dzikoysk/reposilite/issues/145
        if (path.contains(".") && !path.contains("/")) {
            processedPath = "*/" + path.replace(".", "/")
        }

        // Fix non-functional wildcard usage
        // ~ https://github.com/dzikoysk/reposilite/issues/351
        if (processedPath.endsWith("*")) {
            processedPath = processedPath.substring(0, processedPath.length - 1)
            response.add("(warn) Non-functional wildcard has been removed from the end of the given path")
        }

        if (StringUtils.isEmpty(permissions)) {
            permissions = RoutePermission.toString(RoutePermission.defaultRoutePermissions)
            response.add("Added default permissions: $permissions")
        }

        val previousToken = tokenService.getToken(alias)

        return try {
            val token = tokenService.createToken(processedPath, alias, permissions)
            response.add("Generated new access token for $alias ($processedPath) with '$permissions' permissions")
            response.add(token.key)
            tokenService.saveTokens()
            true
        } catch (ioException: IOException) {
            response.add("Cannot generate token due to: " + ioException.message)
            previousToken.peek { token: Token? ->
                tokenService.addToken(token)
                response.add("The former token has been restored.")
            }
            false
        }
    }
}

@Command(name = "chalias", description = ["Change token alias"])
internal class ChAliasCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<alias>", description = ["alias to update"])
    private val alias: String? = null

    @Parameters(index = "1", paramLabel = "<new alias>", description = ["new token name"])
    private val updatedAlias: String? = null

    override fun execute(output: MutableList<String>): Boolean {
        return tokenService
            .updateToken(alias) { token: Token ->
                output.add("Token alias has been changed from '" + token.alias + "' to '" + updatedAlias + "'")
                token.alias = updatedAlias
            }
            .onError { e: String -> output.add(e) }
            .isOk
    }

}

@Command(name = "chmod", description = ["Change token permissions"])
internal class ChmodCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<alias>", description = ["alias to update"])
    private val alias: String? = null

    @Parameters(index = "1", paramLabel = "<permissions>", description = ["new permissions"])
    private val permissions: String? = null

    override fun execute(output: MutableList<String>): Boolean {
        return tokenService
            .updateToken(alias) { token: Token ->
                output.add("Permissions have been changed from '" + token.permissions + "' to '" + permissions + "'")
                token.permissions = permissions
            }
            .onError { e: String -> output.add(e) }
            .isOk
    }
}

@Command(name = "revoke", description = ["Revoke token"])
internal class RevokeCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<alias>", description = ["alias of token to revoke"])
    private val alias: String? = null

    override fun execute(response: MutableList<String>): Boolean {
        return tokenService.deleteToken(alias)
            .map { token: Token? ->
                try {
                    tokenService.saveTokens()
                    response.add("Token for '$alias' has been revoked")
                    return@map true
                } catch (ioException: IOException) {
                    response.add("Cannot remove token due to: $ioException")
                    response.add("Token has been restored.")
                    tokenService.addToken(token)
                    return@map false
                }
            }
            .orElseGet {
                response.add("Alias '$alias' not found")
                false
            }
    }
}
