/*
 * Copyright (c) 2020-2026 dzikoysk
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
import com.reposilite.auth.Authenticator
import com.reposilite.auth.BasicAuthenticator
import com.reposilite.auth.LdapAuthenticator
import com.reposilite.auth.application.BruteForceProtectionSettings
import com.reposilite.auth.application.LdapSettings
import com.reposilite.auth.application.OAuth2Settings
import com.reposilite.auth.oauth2.OAuth2BearerAuthenticator
import com.reposilite.auth.oauth2.OAuth2Service
import com.reposilite.auth.oauth2.OAuth2SessionAuthenticator
import com.reposilite.auth.oauth2.OAuth2SessionStore
import com.reposilite.auth.oauth2.OAuth2TokenVerifier
import com.reposilite.status.FailureFacade
import com.reposilite.token.specification.AccessTokenSpecification
import org.junit.jupiter.api.BeforeEach
import panda.std.reactive.MutableReference
import panda.std.reactive.toMutableReference

internal abstract class AuthenticationSpecification : AccessTokenSpecification() {

    protected val failureFacade = FailureFacade(logger)
    protected val ldapConfiguration = LdapSettings().toMutableReference()
    protected val bruteForceProtectionSettings: MutableReference<BruteForceProtectionSettings> = BruteForceProtectionSettings().toMutableReference()

    protected val oauth2Settings: MutableReference<OAuth2Settings> = OAuth2Settings().toMutableReference()
    protected val oauth2Provider = FakeOAuth2ProviderClient()
    protected val oauth2Ticker = FakeTicker()
    protected val oauth2Service = OAuth2Service(
        journalist = logger,
        settings = oauth2Settings,
        providerClient = oauth2Provider,
        tokenVerifier = OAuth2TokenVerifier(oauth2Provider, oauth2Settings) { oauth2Ticker.read() / 1_000_000 },
        sessionStore = OAuth2SessionStore(oauth2Ticker),
        accessTokenFacade = accessTokenFacade
    )

    protected lateinit var authenticationFacade: AuthenticationFacade

    @BeforeEach
    fun createAuthenticationFacade() {
        this.authenticationFacade = AuthenticationFacade(
            journalist = logger,
            authenticators = sortedSetOf(
                Authenticator.priorityComparator,
                OAuth2SessionAuthenticator(oauth2Service),
                BasicAuthenticator(accessTokenFacade),
                OAuth2BearerAuthenticator(oauth2Service),
                LdapAuthenticator(
                    journalist = logger,
                    ldapSettings = ldapConfiguration,
                    accessTokenFacade = accessTokenFacade,
                    failureFacade = failureFacade
                )
            ),
            accessTokenFacade = accessTokenFacade,
            bruteForceProtectionSettings = bruteForceProtectionSettings
        )
    }

}
