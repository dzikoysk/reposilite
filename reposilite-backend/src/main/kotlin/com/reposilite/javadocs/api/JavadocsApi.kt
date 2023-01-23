package com.reposilite.javadocs.api

import com.reposilite.maven.Repository
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier

data class JavadocPageRequest(
    val accessToken: AccessTokenIdentifier?,
    val repository: Repository,
    val gav: Location
)

data class JavadocResponse(
    val contentType: String,
    val content: String
)