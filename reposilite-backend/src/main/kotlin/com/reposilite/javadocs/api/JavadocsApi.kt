package com.reposilite.javadocs.api

import com.reposilite.maven.Repository
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import io.javalin.http.ContentType
import java.io.InputStream

data class JavadocPageRequest(
    val accessToken: AccessTokenIdentifier?,
    val repository: Repository,
    val gav: Location
)

data class JavadocRawRequest(
    val accessToken: AccessTokenIdentifier?,
    val repository: Repository,
    val gav: Location,
    val resource: Location
)

data class JavadocResponse(
    val contentType: String,
    val content: String
)

data class JavadocRawResponse(
    val contentType: ContentType,
    val content: InputStream
)