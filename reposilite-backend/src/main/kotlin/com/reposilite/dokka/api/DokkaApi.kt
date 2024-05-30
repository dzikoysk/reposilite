package com.reposilite.dokka.api

import com.reposilite.maven.Repository
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import io.javalin.http.ContentType
import java.io.InputStream

data class DokkaPageRequest(
    val accessToken: AccessTokenIdentifier?,
    val repository: Repository,
    val gav: Location
)

data class DokkaRawRequest(
    val accessToken: AccessTokenIdentifier?,
    val repository: Repository,
    val gav: Location,
    val resource: Location
)

data class DokkaResponse(
    val contentType: String,
    val content: String
)

data class DokkaRawResponse(
    val contentType: ContentType,
    val content: InputStream
)