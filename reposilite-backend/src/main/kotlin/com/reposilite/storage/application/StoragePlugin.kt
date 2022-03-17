package com.reposilite.storage.application

import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.storage.StorageFacade

@Plugin(name = "storage")
class StoragePlugin : ReposilitePlugin() {
    override fun initialize(): Facade = StorageFacade()
}
