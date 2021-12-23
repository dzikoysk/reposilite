package com.reposilite.plugin

import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.shared.fs.getSimpleName
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.ServiceLoader
import java.util.stream.Collectors
import kotlin.reflect.full.findAnnotation

private data class PluginEntry(
    val metadata: Plugin,
    val plugin: ReposilitePlugin
)

internal class PluginLoader(
    private val pluginDirectory: Path,
    private val extensionsManagement: ExtensionsManagement
) {

    private val plugins: MutableList<PluginEntry> = mutableListOf()

    fun initialize() = plugins.asSequence()
        .map { it.metadata }
        .associateBy({ it.name }, { it.dependencies.toList() })
        .let { toFlattenedDependencyGraph(it) }
        .map { plugins.first { plugin -> plugin.metadata.name == it } }
        .also {
            extensionsManagement.logger.info("")
            extensionsManagement.logger.info("--- Loading plugins (${it.size}):")
            extensionsManagement.logger.info(it.joinToString(", ", transform = { (metadata, _) -> metadata.name }))
        }
        .forEach { (_, plugin) -> plugin.initialize()?.apply { extensionsManagement.registerFacade(this) } }

    fun registerPlugin(plugin: ReposilitePlugin) {
        val field = plugin::class.java.superclass.getDeclaredField("extensionsManagement")
        field.isAccessible = true
        field.set(plugin, extensionsManagement)
        plugins.add(PluginEntry(plugin::class.findAnnotation() ?: throw IllegalStateException("Plugin ${plugin::class} does not have @Plugin annotation"), plugin))
    }

    fun loadExternalPlugins() {
        if (Files.notExists(pluginDirectory)) {
            Files.createDirectories(pluginDirectory)
        }

        if (!Files.isDirectory(pluginDirectory)) {
            throw IllegalStateException("The path is not a directory")
        }

        Files.list(pluginDirectory)
            .collect(Collectors.toList())
            .filter { it.getSimpleName().endsWith(".jar") }
            .map { it.toUri().toURL() }
            .let { URLClassLoader(it.toTypedArray()) }
            .let { ServiceLoader.load(ReposilitePlugin::class.java, it) }
            .forEach { registerPlugin(it) }
    }

}