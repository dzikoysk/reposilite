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

package com.reposilite.auth.specification

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.token.specification.AccessTokenSpecification

internal abstract class AuthenticationSpecification : AccessTokenSpecification() {

    private val logger = InMemoryLogger()

    protected val authenticationFacade = AuthenticationFacade(
        logger,
        accessTokenFacade
    )

}