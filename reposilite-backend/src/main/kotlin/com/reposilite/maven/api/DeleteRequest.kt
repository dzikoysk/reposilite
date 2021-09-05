package com.reposilite.maven.api

import com.reposilite.token.api.AccessToken

class DeleteRequest(
    val accessToken: AccessToken,
    val repository: String,
    val gav: String
)