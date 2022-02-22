package com.reposilite.settings.api

import java.io.Serializable

/**
 * List of supported Maven repositories.
 */
data class RepositorySettings(
    /**
     * The visibility of this repository
     */
    val visibility: Visibility,
    /**
     * Does this repository accept redeployment of the same artifact version.
     */
    val redeployment: Boolean,
    /**
     * How many builds of the given snapshot version should be preserved when a new build is deployed. Use -1 to disable this feature.
     */
    val preserved: Int,
    /**
     * The storage type of this repository.
     */
    val storageProvider: StorageProvider,
    /**
     * List of proxied repositories associated with this repository.
     */
    val proxied: List<ProxiedRepository>
) : Serializable {
    init {
        require(preserved >= -1L) { "preserved < minimum -1 - $preserved" }
    }

    enum class Visibility : Serializable {
        PUBLIC,
        HIDDEN,
        PRIVATE
    }

    /**
     * Used storage type.
     */
    interface StorageProvider : Serializable {
        val type: String
    }

    data class ProxiedRepository(
        /** The reference to the proxied repository. Either the id of another local repository or the url of a remote repository. */
        val reference: String,
        /** Reposilite can store proxied artifacts locally to reduce response time and improve stability. */
        val store: Boolean,
        /** How long Reposilite can wait for establishing the connection with a remote host. */
        val connectTimeout: Long,
        /** How long Reposilite can read data from remote proxy. */
        val readTimeout: Long,
        /** The authorisation information of the proxied repository */
        val authorization: Authorization?,
        /** Allowed artifact groups. If none are given, all artifacts can be obtained from this proxy. */
        val allowedGroups: List<String>
    ) : Serializable {
        data class Authorization(
            val name: String,
            val token: String
        )
    }
}



