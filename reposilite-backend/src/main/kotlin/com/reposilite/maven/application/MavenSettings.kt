package com.reposilite.maven.application

import com.reposilite.auth.api.Credentials
import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.Min
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.maven.RepositoryVisibility
import com.reposilite.maven.RepositoryVisibility.PRIVATE
import com.reposilite.maven.RepositoryVisibility.PUBLIC
import com.reposilite.storage.StorageProviderSettings
import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings

@Doc(title = "Maven", description = "Repositories settings")
data class MavenSettings(
    @Doc(title = "Repositories", description = "List of Maven repositories.")
    val repositories: List<RepositorySettings> = listOf(
        RepositorySettings("releases"),
        RepositorySettings("snapshots"),
        RepositorySettings("private", visibility = PRIVATE),
    )
) : SharedSettings

@Doc(title = "Maven Repository", description = "Settings for a given repository.")
data class RepositorySettings(
    @Min(1)
    @Doc(title = "Id", description = "The id of this repository.")
    val id: String = "",
    @Doc(title = "Visibility", description = "The visibility of this repository.")
    val visibility: RepositoryVisibility = PUBLIC,
    @Doc(title = "Redeployment", description = "Does this repository accept redeployment of the same artifact version.")
    val redeployment: Boolean = false,
    @Doc(title = "Preserved snapshots", "By default Reposilite deletes all deprecated build files. If you'd like to preserve them, set this property to true.")
    val preserveSnapshots: Boolean = false,
    @Doc(title = "Storage provider", description = "The storage type of this repository.")
    val storageProvider: StorageProviderSettings = FileSystemStorageProviderSettings(),
    @Doc(title = "Mirrored repositories", description = "List of mirrored repositories associated with this repository.")
    val proxied: List<ProxiedRepository> = listOf()
) : SharedSettings

@Doc(title = "Mirrored Maven Repository", description = "Configuration of proxied host")
data class ProxiedRepository(
    @Min(1)
    @Doc(title = "Link", description = "Either the id of other local repository or the URL of a remote repository.")
    val reference: String = "",
    @Doc(title = "Store", description = "Reposilite can store proxied artifacts locally to reduce response time and improve stability.")
    val store: Boolean = false,
    @Doc(title = "Allowed Groups", description = "Allowed artifact groups. If none are given, all artifacts can be obtained from this proxy.")
    val allowedGroups: List<String> = listOf(),
    @Min(0)
    @Doc(title = "Connect Timeout", description = "How long Reposilite can wait for establishing the connection with a remote host.")
    val connectTimeout: Int = 3,
    @Min(0)
    @Doc(title = "Read Timeout", description = "How long Reposilite can read data from remote proxy.")
    val readTimeout: Int = 15,
    // Adding:
    // @Doc(title = "Authorization", description = "The authorization information of the proxied repository.")
    // Results in converting 'authorization` property into 'allOf` component that is currently broken
    // ~ https://github.com/dzikoysk/reposilite/issues/1320
    val authorization: ProxiedCredentials? = null,
    @Doc(title = "HTTP Proxy", description = """
        Custom proxy configuration for HTTP/SOCKS client used by Reposilite. Examples: <br/>
        HTTP 127.0.0.1:1081 <br/>
        SOCKS 127.0.0.1:1080 login password 
    """)
    val httpProxy: String = ""
) : SharedSettings

@Doc(title = "Proxied credentials", description = "The authorization credentials used to access proxied repository.")
data class ProxiedCredentials(
    @Doc(title = "Login", description = "Login to use by proxied HTTP client")
    val login: String = "",
    @Doc(title = "Password", description = "Raw password used by proxied HTTP client to connect to the given repository")
    val password: String = ""
) : SharedSettings {

    fun toCredentials(): Credentials =
        Credentials(login, password)

}
