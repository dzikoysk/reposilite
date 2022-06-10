package com.reposilite.storage

import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin

@Plugin(name = "storage")
class StoragePlugin : ReposilitePlugin() {

    override fun initialize(): Facade =
        StorageFacade()

}
