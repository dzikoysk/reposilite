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

package com.reposilite.token.application

import com.reposilite.console.ConsoleFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.ChModCommand
import com.reposilite.token.ChNameCommand
import com.reposilite.token.KeygenCommand
import com.reposilite.token.RevokeCommand
import com.reposilite.token.TokensCommand
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.infrastructure.InMemoryAccessTokenRepository
import com.reposilite.token.infrastructure.SqlAccessTokenRepository
import com.reposilite.web.ReposiliteRoutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database

internal object AccessTokenWebConfiguration {

    fun createFacade(dispatcher: CoroutineDispatcher, database: Database): AccessTokenFacade =
        AccessTokenFacade(
            temporaryRepository = InMemoryAccessTokenRepository(),
            persistentRepository = SqlAccessTokenRepository(dispatcher, database)
        )

    fun initialize(accessTokenFacade: AccessTokenFacade, temporaryTokens: Collection<CreateAccessTokenRequest>, consoleFacade: ConsoleFacade) {
        temporaryTokens.forEach {
            runBlocking {
                accessTokenFacade.createTemporaryAccessToken(it)
            }
        }

        consoleFacade.registerCommand(TokensCommand(accessTokenFacade))
        consoleFacade.registerCommand(KeygenCommand(accessTokenFacade))
        consoleFacade.registerCommand(ChNameCommand(accessTokenFacade))
        consoleFacade.registerCommand(ChModCommand(accessTokenFacade))
        consoleFacade.registerCommand(RevokeCommand(accessTokenFacade))
    }

    fun routing(): Set<ReposiliteRoutes> = emptySet()

}