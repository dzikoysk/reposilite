package com.reposilite.maven.application

import com.reposilite.auth.api.Credentials
import com.reposilite.maven.RepositoryVisibility
import com.reposilite.maven.RepositoryVisibility.PRIVATE
import com.reposilite.maven.RepositoryVisibility.PUBLIC
import com.reposilite.settings.api.Doc
import com.reposilite.settings.api.Min
import com.reposilite.storage.application.FileSystemStorageProviderSettings
import com.reposilite.storage.application.StorageProviderSettings
import java.io.Serializable

@Doc(title = "Repositories", description = "Repositories settings")
data class RepositoriesSettings(
    @Doc(title = "Repositories", description = "List of Maven repositories.")
    val repositories: Map<String, RepositorySettings> = mapOf(
        "releases" to RepositorySettings(),
        "snapshots" to RepositorySettings(),
        "private" to RepositorySettings(visibility = PRIVATE)
    )
)

@Doc(title = "Repository", description = "Settings for a Repository.")
data class RepositorySettings(
    /** The visibility of this repository */
    @Doc(title = "Visibility", description = "The visibility of this repository")
    val visibility: RepositoryVisibility = PUBLIC,
    /** Does this repository accept redeployment of the same artifact version. */
    @Doc(title = "Redeployment", description = "Does this repository accept redeployment of the same artifact version.")
    val redeployment: Boolean = false,
    /** How many builds of the given snapshot version should be preserved when a new build is deployed. Use -1 to disable this feature. */
    @Min(-1)
    @Doc(title = "Preserved", description = "How many builds of the given snapshot version should be preserved when a new build is deployed. Use -1 to disable this feature.")
    val preserved: Int = -1,
    /** The storage type of this repository. */
    val storageProvider: StorageProviderSettings = FileSystemStorageProviderSettings("100%", ""),
    /** List of proxied repositories associated with this repository. */
    @Doc(title = "Proxied", description = "List of proxied repositories associated with this repository.")
    val proxied: List<ProxiedRepository> = listOf()
) : Serializable {

    init {
        require(preserved >= -1L) { "Number of preserved snapshot builds cannot be smaller than -1 (now: $preserved)" }
        require(preserved != 0) { "Number of preserved snapshot builds has to be greater than 0" }
    }

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
    val authorization: Credentials? = null,
    /** Allowed artifact groups. If none are given, all artifacts can be obtained from this proxy. */
    @Doc(title = "Allowed Groups", description = "Allowed artifact groups. If none are given, all artifacts can be obtained from this proxy.")
    val allowedGroups: List<String> = listOf(),
    /**  */
    @Doc(title = "Proxy", description = "")
    val proxy: String = ""
)


