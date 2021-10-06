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

package com.reposilite.token.specification

import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.CreateAccessTokenResponse
import com.reposilite.token.infrastructure.InMemoryAccessTokenRepository

internal open class AccessTokenSpecification {

    protected val accessTokenFacade = AccessTokenFacade(InMemoryAccessTokenRepository(), InMemoryAccessTokenRepository())

    protected fun createToken(name: String): CreateAccessTokenResponse =
        accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name))

    protected fun createToken(name: String, secret: String): AccessToken =
        accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name, secret)).accessToken

}