package com.reposilite.maven.application

import java.io.Serializable

data class RepositoriesSettings(val repositories: Map<String, RepositorySettings> = mapOf(
    "releases" to RepositorySettings(),
    "snapshots" to RepositorySettings(),
    "private" to RepositorySettings(visibility = RepositorySettings.Visibility.PRIVATE)
)) : Serializable
