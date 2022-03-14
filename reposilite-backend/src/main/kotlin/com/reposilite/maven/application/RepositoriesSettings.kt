package com.reposilite.maven.application

import com.reposilite.settings.api.Doc
import java.io.Serializable

@Doc(title = "Repositories", description = "Repositories settings")
data class RepositoriesSettings(
    @Doc(title = "Repositories", description = "List of Maven repositories.")
    val repositories: Map<String, RepositorySettings> = mapOf(
        "releases" to RepositorySettings(),
        "snapshots" to RepositorySettings(),
        "private" to RepositorySettings(visibility = RepositorySettings.Visibility.PRIVATE)
    )
) : Serializable
