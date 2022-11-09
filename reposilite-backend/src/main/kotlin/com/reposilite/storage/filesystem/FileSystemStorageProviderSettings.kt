package com.reposilite.storage.filesystem

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.storage.StorageProviderSettings
import io.javalin.openapi.Custom

@Doc(title = "File system Storage Provider", description = "Local file system (disk) storage provider settings")
data class FileSystemStorageProviderSettings(
    @get:Custom(name = "const", value = "fs")
    override val type: String = "fs",
    @get:Doc(title = "Quota", description = "Control the maximum amount of data stored in this repository. Supported formats: 90%, 500MB, 10GB (optional, by default: unlimited)")
    val quota: String = "100%",
    @get:Doc(title = "Mount", description = "Use custom directory to locate the repository data (optional, by default it's './repositories/{name}')")
    val mount: String = ""
) : StorageProviderSettings
