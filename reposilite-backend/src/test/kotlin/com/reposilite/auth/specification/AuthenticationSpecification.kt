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
import com.reposilite.auth.BasicAuthenticator
import com.reposilite.auth.LdapAuthenticator
import com.reposilite.settings.api.SharedConfiguration.LdapConfiguration
import com.reposilite.token.specification.AccessTokenSpecification
import panda.std.reactive.toReference

internal abstract class AuthenticationSpecification : AccessTokenSpecification() {

    protected val ldapConfiguration = LdapConfiguration().toReference()

    protected val authenticationFacade = AuthenticationFacade(
        journalist = logger,
        authenticators = listOf(
            BasicAuthenticator(accessTokenFacade),
            LdapAuthenticator(ldapConfiguration, accessTokenFacade)
        ),
        accessTokenFacade = accessTokenFacade
    )

}