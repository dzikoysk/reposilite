package com.reposilite.configuration.local

import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.parameters

@Plugin(name = "local-configuration", dependencies = ["configuration"])
class LocalConfigurationPlugin : ReposilitePlugin() {

    override fun initialize(): LocalConfiguration =
        LocalConfigurationFactory.createLocalConfiguration(this, parameters())

}