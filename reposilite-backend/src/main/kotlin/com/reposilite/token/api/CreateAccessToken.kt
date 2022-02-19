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

package com.reposilite.token.api

import com.reposilite.token.AccessTokenType

data class CreateAccessTokenRequest(
    val type: AccessTokenType,
    val name: String,
    val secret: String? = null
)

data class CreateAccessTokenWithNoNameRequest(
    val type: AccessTokenType,
    val secret: String? = null,
    val permissions: Set<String>
)

data class CreateAccessTokenResponse(
    val accessToken: AccessTokenDto,
    val secret: String,
)
