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

package com.reposilite.auth.application

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.SessionService
import com.reposilite.auth.infrastructure.AuthenticationEndpoint
import com.reposilite.auth.infrastructure.PostAuthHandler
import com.reposilite.token.AccessTokenFacade
import com.reposilite.web.ReposiliteRoutes
import net.dzikoysk.dynamiclogger.Journalist

internal object AuthenticationWebConfiguration {

    fun createFacade(journalist: Journalist, accessTokenFacade: AccessTokenFacade): AuthenticationFacade =
        AuthenticationFacade(journalist, accessTokenFacade, SessionService())

    fun routing(authenticationFacade: AuthenticationFacade): Set<ReposiliteRoutes> = setOf(
        AuthenticationEndpoint(authenticationFacade),
        PostAuthHandler()
    )

}