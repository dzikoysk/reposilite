package com.reposilite.token.api

data class CreateAccessTokenResponse(
    val accessToken: AccessToken,
    val secret: String,
)
