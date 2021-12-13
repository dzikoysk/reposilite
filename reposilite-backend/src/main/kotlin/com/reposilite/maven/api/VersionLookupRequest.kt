package com.reposilite.maven.api

import com.reposilite.token.api.AccessToken

data class VersionLookupRequest(
        val accessToken: AccessToken?,
        val repository: String,
        val gav: String,
        val filter: String?
)