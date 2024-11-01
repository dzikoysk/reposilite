package com.reposilite.javadocs.api

import com.reposilite.packages.maven.MavenRepository
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import io.javalin.http.ContentType
import java.io.InputStream

data class JavadocPageRequest(
    val accessToken: AccessTokenIdentifier?,
    val mavenRepository: MavenRepository,
    val gav: Location
)

data class JavadocRawRequest(
    val accessToken: AccessTokenIdentifier?,
    val mavenRepository: MavenRepository,
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