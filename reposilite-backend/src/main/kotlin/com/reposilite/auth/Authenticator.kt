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

package com.reposilite.auth

import com.reposilite.auth.api.Credentials
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.web.http.ErrorResponse
import panda.std.Result

interface Authenticator {

    fun authenticate(credentials: Credentials): Result<AccessTokenDto, ErrorResponse>

    fun enabled(): Boolean

    fun realm(): String

}