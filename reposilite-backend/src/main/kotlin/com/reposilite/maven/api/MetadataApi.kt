package com.reposilite.maven.api

import com.reposilite.maven.Repository
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier

data class SaveMetadataRequest(
    val repository: Repository,
    val gav: Location,
    val metadata: Metadata
)

data class GeneratePomRequest(
    val accessToken: AccessTokenIdentifier,
    val repository: Repository,
    val gav: Location,
    val pomDetails: PomDetails
)

data class PomDetails(
    val groupId: String,
    val artifactId: String,
    val version: String
)
