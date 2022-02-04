package com.reposilite.plugin.groovy

import com.reposilite.plugin.PluginLoader
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.storage.getSimpleName
import groovy.lang.GroovyClassLoader
import java.nio.file.Files

@Plugin(name = "groovy")
class GroovyPlugin : ReposilitePlugin() {

    override fun load(loader: PluginLoader) {
        val groovyClassLoader = GroovyClassLoader(this::class.java.classLoader)

        Files.list(loader.pluginDirectory).use { pluginDirectoryStream ->
            pluginDirectoryStream
                .filter { it.getSimpleName().endsWith(".groovy") }
                .map { groovyClassLoader.parseClass(it.toFile()) }
                .forEach { loader.registerPlugin(it.getConstructor().newInstance() as ReposilitePlugin) }
        }
    }

    override fun initialize(): Facade? =
        null

}