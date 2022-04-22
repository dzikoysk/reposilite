/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.plugin

import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.api.ReposilitePlugin.ReposilitePluginAccessor
import com.reposilite.storage.getSimpleName
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import kotlin.reflect.full.findAnnotation

private data class PluginEntry(
    val metadata: Plugin,
    val plugin: ReposilitePlugin
)

class PluginLoader(
    val pluginDirectory: Path,
    val extensions: Extensions
) {

    private val plugins: MutableList<PluginEntry> = mutableListOf()

    fun initialize() = plugins.toList()
        .forEach { it.plugin.load(this) }
        .let { plugins.asSequence() }
        .map { it.metadata }
        .associateBy({ it.name }, { it.dependencies.toList() })
        .let { toFlattenedDependencyGraph(it) }
        .map { plugins.first { plugin -> plugin.metadata.name == it } }
        .also {
            extensions.logger.info("")
            extensions.logger.info("--- Loading plugins (${it.size}):")
            extensions.logger.info(it.joinToString(", ", transform = { (metadata, _) -> metadata.name }))
        }
        .forEach { (_, plugin) -> plugin.initialize()?.apply { extensions.registerFacade(this) } }

    fun registerPlugin(plugin: ReposilitePlugin) {
        plugin::class.findAnnotation<Plugin>()
            ?.let { PluginEntry(it, plugin) }
            ?.also { ReposilitePluginAccessor.injectExtension(it.plugin, extensions) }
            ?.also { plugins.add(it) }
            ?: throw IllegalStateException("Plugin ${plugin::class} does not have @Plugin annotation")
    }

}

internal fun PluginLoader.loadExternalPlugins() {
    if (Files.notExists(pluginDirectory)) {
        Files.createDirectories(pluginDirectory)
    }

    if (!Files.isDirectory(pluginDirectory)) {
        throw IllegalStateException("The path is not a directory")
    }

    Files.list(pluginDirectory).use { pluginDirectoryStream ->
        pluginDirectoryStream
            .collect(Collectors.toList())
            .filter { it.getSimpleName().endsWith(".jar") }
            .map { it.toUri().toURL() }
            .let { URLClassLoader(it.toTypedArray()) }
            .let { ServiceLoader.load(ReposilitePlugin::class.java, it) }
            .forEach { registerPlugin(it) }
    }
}
