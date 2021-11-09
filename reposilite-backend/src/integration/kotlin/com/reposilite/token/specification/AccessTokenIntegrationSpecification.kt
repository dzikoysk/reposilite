package com.reposilite.token.specification

import com.reposilite.ReposiliteSpecification
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.CreateAccessTokenRequest

internal abstract class AccessTokenIntegrationSpecification : ReposiliteSpecification() {

    protected fun useToken(name: String, secret: String) =
        Pair(
            reposilite.accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name, secret)).accessToken,
            secret
        )

    protected fun useTokenDescription(name: String, secret: String, permissions: Set<AccessTokenPermission> = emptySet()) =
        Triple(name, secret, permissions)

}