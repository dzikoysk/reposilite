package com.reposilite.plugin

import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import kotlin.reflect.full.findAnnotation

private data class PluginEntry(
    val metadata: Plugin,
    val plugin: ReposilitePlugin
)

internal class PluginLoader(private val extensionsManagement: ExtensionsManagement) {

    private val plugins: MutableList<PluginEntry> = mutableListOf()

    fun initialize() = plugins.asSequence()
        .map { it.metadata }
        .associateBy({ it.name }, { it.dependencies.toList() })
        .let { toFlattenedDependencyGraph(it) }
        .map { plugins.first { plugin -> plugin.metadata.name == it } }
        .also {
            extensionsManagement.logger.info("")
            extensionsManagement.logger.info("--- Loading plugins (${it.size}):")
            extensionsManagement.logger.info(it.joinToString(", ", transform = { (metadata, _) -> metadata.name}))
        }
        .forEach { (_, plugin) -> plugin.initialize()?.apply { extensionsManagement.registerFacade(this) } }

    fun registerPlugin(plugin: ReposilitePlugin) {
        val field = plugin::class.java.superclass.getDeclaredField("extensionsManagement")
        field.isAccessible = true
        field.set(plugin, extensionsManagement)
        plugins.add(PluginEntry(plugin::class.findAnnotation()!!, plugin))
    }

    fun registerPluginsFromFile() {

    }

}