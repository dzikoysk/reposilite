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

import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.storage.getSimpleName
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.ServiceLoader
import java.util.stream.Collectors

class PluginLoader(
    val pluginDirectory: Path,
    val extensions: Extensions
) {

    fun initialize() {
        extensions.getPlugins().values
            .forEach { (_, plugin) -> plugin.load(this) }

        val plugins = sortPlugins()

        extensions.logger.info("")
        extensions.logger.info("--- Loading plugins (${plugins.size}):")

        plugins.chunked(5).forEach {
            extensions.logger.info(it.joinToString(", ", transform = { (metadata, _) -> metadata.name }))
        }

        plugins.forEach { (_, plugin) ->
            plugin.initialize()?.apply { extensions.registerFacade(this) }
        }
    }

    private fun sortPlugins(): List<PluginEntry> =
        with (extensions.getPlugins()) {
            extensions.getPlugins().asSequence()
                .map { it.value.metadata }
                .associateBy({ it.name }, { it.dependencies.toList() })
                .let { toFlattenedDependencyGraph(it) }
                .map { this[it]!! }
        }

    internal fun loadExternalPlugins() {
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
                .forEach { extensions.registerPlugin(it) }
        }
    }

}