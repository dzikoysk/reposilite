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

import net.dzikoysk.dynamiclogger.Journalist
import com.reposilite.console.ConsoleFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.ChAliasCommand
import com.reposilite.token.ChModCommand
import com.reposilite.token.KeygenCommand
import com.reposilite.token.RevokeCommand
import com.reposilite.token.TokensCommand
import com.reposilite.token.infrastructure.SqlAccessTokenRepository

internal object AccessTokenWebConfiguration {

    fun createFacade(journalist: Journalist): AccessTokenFacade =
        AccessTokenFacade(journalist, SqlAccessTokenRepository())

    fun initialize(accessTokenFacade: AccessTokenFacade, consoleFacade: ConsoleFacade) {
        consoleFacade.registerCommand(TokensCommand(accessTokenFacade))
        consoleFacade.registerCommand(KeygenCommand(accessTokenFacade))
        consoleFacade.registerCommand(ChAliasCommand(accessTokenFacade))
        consoleFacade.registerCommand(ChModCommand(accessTokenFacade))
        consoleFacade.registerCommand(RevokeCommand(accessTokenFacade))
    }

}