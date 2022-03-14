package com.reposilite.maven.application

import com.reposilite.settings.api.Doc
import com.reposilite.settings.api.Min
import java.io.Serializable

@Doc(title = "Repository", description = "Settings for a Repository.")
data class RepositorySettings(
    /** The visibility of this repository */
    @Doc(title = "Visibility", description = "The visibility of this repository")
    val visibility: Visibility = Visibility.PUBLIC,
    /** Does this repository accept redeployment of the same artifact version. */
    @Doc(title = "Redeployment", description = "Does this repository accept redeployment of the same artifact version.")
    val redeployment: Boolean = false,
    /** How many builds of the given snapshot version should be preserved when a new build is deployed. Use -1 to disable this feature. */
    @Min(-1)
    @Doc(title = "Preserved", description = "How many builds of the given snapshot version should be preserved when a new build is deployed. Use -1 to disable this feature.")
    val preserved: Int = -1,
    /** The storage type of this repository. */
    val storageProvider: StorageProvider = FSStorageProviderSettings("100%", ""),
    /** List of proxied repositories associated with this repository. */
    @Doc(title = "Proxied", description = "List of proxied repositories associated with this repository.")
    val proxied: List<ProxiedRepository> = listOf()
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
        @Doc(title = "Reference", description = "The reference to the proxied repository. Either the id of another local repository or the url of a remote repository.")
        val reference: String,
        /** Reposilite can store proxied artifacts locally to reduce response time and improve stability. */
        @Doc(title = "Store", description = "Reposilite can store proxied artifacts locally to reduce response time and improve stability.")
        val store: Boolean = false,
        /** How long Reposilite can wait for establishing the connection with a remote host. */
        @Min(0)
        @Doc(title = "Connect Timeout", description = "How long Reposilite can wait for establishing the connection with a remote host.")
        val connectTimeout: Int = 3,
        /** How long Reposilite can read data from remote proxy. */
        @Min(0)
        @Doc(title = "Read Timeout", description = "How long Reposilite can read data from remote proxy.")
        val readTimeout: Int = 15,
        /** The authorisation information of the proxied repository */
        @Doc(title = "Authorisation", description = "The authorisation information of the proxied repository.")
        val authorization: Authorization? = null,
        /** Allowed artifact groups. If none are given, all artifacts can be obtained from this proxy. */
        @Doc(title = "Allowed Groups", description = "Allowed artifact groups. If none are given, all artifacts can be obtained from this proxy.")
        val allowedGroups: List<String> = listOf(),
        /**  */
        @Doc(title = "Proxy", description = "")
        val proxy: String = ""
    ) : Serializable {

        data class Authorization(
            val name: String,
            val token: String
        )
    }
}



