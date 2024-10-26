/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.token.application

import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.parameters
import com.reposilite.plugin.reposilite
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.infrastructure.AccessTokenApiEndpoints
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "access-token")
internal class AccessTokenPlugin : ReposilitePlugin() {

    companion object {
        const val MAX_TOKEN_NAME = 255
        const val MAX_ROUTE_LENGTH = 1024
    }

    override fun initialize(): AccessTokenFacade {
        val accessTokenFacade = AccessTokenComponents(
            journalist = this,
            database = reposilite().database
        ).accessTokenFacade()

        parameters().tokens.forEach {
            val (token) = accessTokenFacade.createAccessToken(it)
            accessTokenFacade.addPermission(token.identifier, MANAGER)
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(AccessTokenApiEndpoints(accessTokenFacade))
        }

        event { _: ReposiliteInitializeEvent ->
            if (accessTokenFacade.count() == 0L) {
                logger.info("")
                logger.info("--- Access Tokens")
                logger.info("Your instance does not have any access tokens yet.")
                logger.info("To generate token, you can use --token flag to create temporary token:")
                logger.info("$ java -jar reposilite.jar --token name:secret")
                logger.info("Or using command in this terminal/through web dashboard:")
                logger.info("$ help token-generate")
            }
        }

        return accessTokenFacade
    }

}
