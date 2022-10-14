package com.reposilite.plugin.mavenindexer

import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin

@Plugin(name = "maven-indexer")
internal class MavenIndexerPlugin : ReposilitePlugin() {
    override fun initialize(): Facade? {
        extensions().registerEvent(
            ReposiliteInitializeEvent::class.java
        ) {
            logger.info("")
            logger.info("--- Maven Indexer plugin")
            logger.info("Maven Indexer plugin has been properly loaded")
        }
        return null
    }

}