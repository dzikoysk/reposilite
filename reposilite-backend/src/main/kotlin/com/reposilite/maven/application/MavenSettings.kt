package com.reposilite.maven.application

import com.reposilite.auth.api.Credentials
import com.reposilite.maven.RepositoryVisibility
import com.reposilite.maven.RepositoryVisibility.PRIVATE
import com.reposilite.maven.RepositoryVisibility.PUBLIC
import com.reposilite.settings.api.Doc
import com.reposilite.settings.api.Min
import com.reposilite.settings.api.Settings
import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import com.reposilite.storage.application.StorageProviderSettings
import com.reposilite.storage.application.StorageProviderSettingsComposer
import net.dzikoysk.cdn.entity.Contextual
import net.dzikoysk.cdn.entity.CustomComposer

@Contextual
@Doc(title = "Maven Repositories", description = "Repositories settings")
data class MavenSettings(
    @Doc(title = "Repositories", description = "List of Maven repositories.")
    val repositories: List<RepositorySettings> = listOf(
        RepositorySettings("releases"),
        RepositorySettings("snapshots"),
        RepositorySettings("private", visibility = PRIVATE),
//        RepositorySettings("private2", visibility = PRIVATE, proxied = listOf(ProxiedRepository("private")))
    )
) : Settings

@Contextual
@Doc(title = "Maven Repository", description = "Settings for a given repository.")
data class RepositorySettings(
    @Doc(title = "Id", description = "The id of this repository.")
    val id: String,
    @Doc(title = "Visibility", description = "The visibility of this repository.")
    val visibility: RepositoryVisibility = PUBLIC,
    @Doc(title = "Redeployment", description = "Does this repository accept redeployment of the same artifact version.")
    val redeployment: Boolean = false,
    @Min(-1)
    @Doc(title = "Preserved", description = "How many builds of the given snapshot version should be preserved when a new build is deployed. Use -1 to disable this feature.")
    val preserved: Int = -1,
    @Doc(title = "Storage provider", description = "The storage type of this repository.")
    @CustomComposer(StorageProviderSettingsComposer::class)
    val storageProvider: StorageProviderSettings = FileSystemStorageProviderSettings(),
    @Doc(title = "Proxied", description = "List of proxied repositories associated with this repository.")
    val proxied: List<ProxiedRepository> = listOf()
) : Settings {

    init {
        require(preserved >= -1L) { "Number of preserved snapshot builds cannot be smaller than -1 (now: $preserved)" }
        require(preserved != 0) { "Number of preserved snapshot builds has to be greater than 0 or -1 to make unlimited" }
    }

}

@Contextual
@Doc(title = "Proxied Maven Repository", description = "Configuration of proxied host")
data class ProxiedRepository(
    @Doc(title = "Reference", description = "The reference to the proxied repository. Either the id of another local repository or the url of a remote repository.")
    val reference: String = "",
    @Doc(title = "Store", description = "Reposilite can store proxied artifacts locally to reduce response time and improve stability.")
    val store: Boolean = false,
    @Min(0)
    @Doc(title = "Connect Timeout", description = "How long Reposilite can wait for establishing the connection with a remote host.")
    val connectTimeout: Int = 3,
    @Min(0)
    @Doc(title = "Read Timeout", description = "How long Reposilite can read data from remote proxy.")
    val readTimeout: Int = 15,
    @Doc(title = "Authorisation", description = "The authorisation information of the proxied repository.")
    val authorization: Credentials? = null,
    @Doc(title = "Allowed Groups", description = "Allowed artifact groups. If none are given, all artifacts can be obtained from this proxy.")
    val allowedGroups: List<String> = listOf(),
    @Doc(title = "Proxy", description = "Custom proxy configuration for HTTP client used by Reposilite")
    val proxy: String = ""
) : Settings
