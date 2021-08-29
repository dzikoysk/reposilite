package com.reposilite.token.api

data class CreateAccessTokenRequest(
    val name: String,
    val secret: String? = null,
    val permissions: Set<AccessTokenPermission> = emptySet()
)

data class CreateAccessTokenResponse(
    val accessToken: AccessToken,
    val secret: String,
)
