package com.reposilite.storage.application

import com.reposilite.settings.api.Doc

@Doc(title = "Filesystem Storage Provider", description = "Local file system (disk) storage provider settings")
data class FSStorageProviderSettings(
    @Doc(title = "Quota", description = "Control the maximum amount of data stored in this repository. Supported formats: 90%, 500MB, 10GB (optional, by default: unlimited)")
    val quota: String = "100%",
    @Doc(title = "Mount", description = "use custom directory to locate the repository data (optional, by default it's './repositories/{name}')")
    val mount: String = ""
): StorageProviderSettings {

    override val type: String =
        "fs"

}
