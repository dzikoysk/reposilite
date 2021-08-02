package org.panda_lang.reposilite.token.api

data class CreateAccessTokenResponse(
    val accessToken: AccessToken,
    val secret: String,
)
