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

package org.panda_lang.reposilite.token

import org.panda_lang.reposilite.console.ReposiliteCommand
import org.panda_lang.reposilite.console.Status
import org.panda_lang.reposilite.console.Status.FAILED
import org.panda_lang.reposilite.console.Status.SUCCEEDED
import org.panda_lang.reposilite.token.api.AccessTokenPermission
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "tokens", description = ["List all generated tokens"])
internal class TokensCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    override fun execute(output: MutableList<String>): Status {
        output.add("Tokens (${accessTokenFacade.count()})")

        accessTokenFacade.getTokens().forEach {
            output.add(it.alias + ":'")

            it.routes.forEach { route ->
                output.add("  ${route.path} : ${route.permissions}")
            }
        }

        return SUCCEEDED
    }

}

@Command(name = "keygen", description = ["Generate a new access token"])
internal class KeygenCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<alias>", description = ["access token alias"])
    private lateinit var alias: String

    @Parameters(
        index = "2",
        paramLabel = "[<permissions>]",
        description = ["extra permissions: m - manager"],
        defaultValue = ""
    )
    private lateinit var permissions: String

    override fun execute(output: MutableList<String>): Status {
       // var processedPath = path

        // Support simplified artifact qualifier (x.y.z instead of /x/y/z)
        // ~ https://github.com/dzikoysk/reposilite/issues/145
//        if (path.contains(".") && !path.contains("/")) {
//            processedPath = "*/" + path.replace(".", "/")
//        }

        // Fix non-functional wildcard usage
        // ~ https://github.com/dzikoysk/reposilite/issues/351
//        if (processedPath.endsWith("*")) {
//            processedPath = processedPath.substring(0, processedPath.length - 1)
//            response.add("(warn) Non-functional wildcard has been removed from the end of the given path")
//        }

        val token = accessTokenFacade.createAccessToken(alias, AccessTokenPermission.ofSymbols(permissions))
        output.add("Generated new access token for $alias with '$permissions' permissions")
        output.add(token.key)

        return SUCCEEDED
    }
}

@Command(name = "chalias", description = ["Change token alias"])
internal class ChAliasCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<alias>", description = ["alias to update"])
    private lateinit var alias: String

    @Parameters(index = "1", paramLabel = "<new alias>", description = ["new token name"])
    private lateinit var updatedAlias: String

    override fun execute(output: MutableList<String>): Status =
        accessTokenFacade.getToken(alias)
            .map {
                accessTokenFacade.updateToken(it.copy(alias = updatedAlias))
                output.add("Token alias has been changed from '$alias' to '$updatedAlias'")
                SUCCEEDED
            }
            .orElseGet {
                output.add("Token '$alias' not found")
                FAILED
            }

}

@Command(name = "chmod", description = ["Change token permissions"])
internal class ChmodCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<alias>", description = ["alias to update"])
    private lateinit var alias: String

    @Parameters(index = "1", paramLabel = "<permissions>", description = ["new permissions"])
    private lateinit var permissions: String

    override fun execute(output: MutableList<String>): Status =
        accessTokenFacade.getToken(alias)
            .map {
                accessTokenFacade.updateToken(it.copy(permissions = permissions))
                output.add("Permissions have been changed from '${it.permissions}' to '$permissions'")
                SUCCEEDED
            }
            .orElseGet {
                output.add("Token '$alias' not found")
                FAILED
            }

}

@Command(name = "revoke", description = ["Revoke token"])
internal class RevokeCommand(private val accessTokenFacade: AccessTokenFacade) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<alias>", description = ["alias of token to revoke"])
    private lateinit var alias: String

    override fun execute(output: MutableList<String>): Status {
        accessTokenFacade.deleteToken(alias)
        output.add("Token for '$alias' has been revoked")
        return SUCCEEDED
    }

}
