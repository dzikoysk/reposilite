/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.maven.application

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.Min
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.maven.StoragePolicy
import com.reposilite.maven.RepositoryVisibility
import com.reposilite.maven.RepositoryVisibility.PRIVATE
import com.reposilite.maven.RepositoryVisibility.PUBLIC
import com.reposilite.shared.http.AuthenticationMethod
import com.reposilite.shared.http.RemoteCredentials
import com.reposilite.storage.StorageProviderSettings
import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import com.reposilite.storage.s3.S3StorageProviderSettings
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OneOf

@JsonSchema(requireNonNulls = false)
@Doc(title = "Maven", description = "Repositories settings")
data class MavenSettings(
    @get:Doc(title = "Repositories", description = "List of Maven repositories.")
    val repositories: List<RepositorySettings> = listOf(
        RepositorySettings("releases"),
        RepositorySettings("snapshots"),
        RepositorySettings("private", visibility = PRIVATE),
    )
) : SharedSettings

@Doc(title = "Maven Repository", description = "Settings for a given repository.")
data class RepositorySettings(
    @Min(1)
    @get:Doc(title = "Id", description = "The id of this repository.")
    val id: String = "",
    @get:Doc(title = "Visibility", description = "The visibility of this repository.")
    val visibility: RepositoryVisibility = PUBLIC,
    @get:Doc(title = "Redeployment", description = "Does this repository accept redeployment of the same artifact version.")
    val redeployment: Boolean = false,
    @get:Doc(title = "Preserved snapshots", "By default, Reposilite deletes all deprecated build files. If you'd like to preserve them, set this property to true.")
    val preserveSnapshots: Boolean = false,
    @get:Doc(title = "Storage provider", description = "The storage type of this repository.")
    @get:OneOf(FileSystemStorageProviderSettings::class, S3StorageProviderSettings::class)
    val storageProvider: StorageProviderSettings = FileSystemStorageProviderSettings(),
    @get:Doc(title = "Storage policy", description = """
        Defines how Reposilite should handle requests to repository with configured mirrored repositories. <br/>
        PRIORITIZE_UPSTREAM_METADATA - try to fetch the latest version of the artifact from the remote repository <br/>
        STRICT - prioritize cached version over upstream metadata (full offline mode)
    """)
    val storagePolicy: StoragePolicy = StoragePolicy.PRIORITIZE_UPSTREAM_METADATA,
    @get:Doc(title = "Max age of metadata file", description = """
        Fetching metadata files with PRIORITIZE_UPSTREAM_METADATA may be slow, so you can use dedicated TTL threshold for them.<br/>
        If set to "0" then a fetch is done always. (Default: 0 seconds)
    """)
    val metadataMaxAge: Long = 0L,
    @get:Doc(title = "Mirrored repositories", description = "List of mirrored repositories associated with this repository.")
    val proxied: List<MirroredRepositorySettings> = listOf()
) : SharedSettings

@Doc(title = "Mirrored Maven Repository", description = "Configuration of proxied host")
data class MirroredRepositorySettings(
    @Min(1)
    @get:Doc(title = "Link", description = "Either the id of other local repository or the URL of a remote repository.")
    val reference: String = "",
    @get:Doc(title = "Store", description = "Reposilite can store proxied artifacts locally to reduce response time and improve stability.")
    val store: Boolean = false,
    @get:Doc(title = "Allowed Groups", description = "Allowed artifact groups. If none are given, all artifacts can be obtained from this mirror.")
    val allowedGroups: List<String> = listOf(),
    @get:Doc(title = "Allowed Extensions", description = "List of accepted file extensions. If none are given, all files can be obtained from this mirror.")
    val allowedExtensions: List<String> = listOf(".jar", ".war", ".pom", ".xml", ".module", ".md5", ".sha1", ".sha256", ".sha512", ".asc"),
    @Min(0)
    @get:Doc(title = "Connect Timeout", description = "How long Reposilite can wait for establishing the connection with a remote host. (In seconds)")
    val connectTimeout: Int = 3,
    @Min(0)
    @get:Doc(title = "Read Timeout", description = "How long Reposilite can read data from remote proxy. (In seconds)")
    val readTimeout: Int = 15,
    // Adding:
    // @Doc(title = "Authorization", description = "The authorization information of the proxied repository.")
    // Results in converting 'authorization` property into 'allOf` component that is currently broken
    // ~ https://github.com/dzikoysk/reposilite/issues/1320
    val authorization: MirrorCredentials? = null,
    @get:Doc(title = "HTTP Proxy", description = """
        Custom proxy configuration for HTTP/SOCKS client used by Reposilite to connect to the mirrored repository. Examples: <br/>
        HTTP 127.0.0.1:1081 <br/>
        SOCKS 127.0.0.1:1080 login password 
    """)
    val httpProxy: String = ""
) : SharedSettings

@Doc(title = "Mirror Credentials", description = "The authorization credentials used to access mirrored repository.")
data class MirrorCredentials(
    @get:Doc(title = "Method", description = "Basic or custom header")
    override val method: AuthenticationMethod = AuthenticationMethod.BASIC,
    @get:Doc(title = "Login", description = "Login or custom header name to use")
    override val login: String = "",
    @get:Doc(title = "Password", description = "Raw password or header value used by HTTP client to connect to the given repository")
    override val password: String = ""
) : SharedSettings, RemoteCredentials
