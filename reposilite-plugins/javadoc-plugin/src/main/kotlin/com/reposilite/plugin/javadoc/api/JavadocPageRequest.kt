package com.reposilite.plugin.javadoc.api

import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier

data class JavadocPageRequest(
    val accessToken: AccessTokenIdentifier?,
    val repository: String,
    val gav: Location
)