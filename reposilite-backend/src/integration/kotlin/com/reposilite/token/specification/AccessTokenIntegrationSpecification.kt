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

package com.reposilite.token.specification

import com.reposilite.specification.ReposiliteSpecification
import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.api.SessionDetails
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.api.CreateAccessTokenRequest

internal abstract class AccessTokenIntegrationSpecification : ReposiliteSpecification() {

    protected fun useToken(name: String, secret: String) =
        useFacade<AccessTokenFacade>().createAccessToken(CreateAccessTokenRequest(PERSISTENT, name, secret = secret)).accessToken to secret

    protected fun useExistingToken(name: String): SessionDetails =
        useFacade<AccessTokenFacade>().getAccessToken(name)!!
            .identifier
            .let { useFacade<AuthenticationFacade>().geSessionDetails(it) }
            .get()

    protected fun useTokenDescription(name: String, secret: String, permissions: Set<AccessTokenPermission> = emptySet()) =
        Triple(name, secret, permissions)

    protected fun getPermissions(identifier: AccessTokenIdentifier): Set<AccessTokenPermission> =
        useFacade<AccessTokenFacade>().getPermissions(identifier)

}