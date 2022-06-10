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
import panda.std.Result
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.ServiceLoader
import java.util.jar.JarFile
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString

class PluginLoader(
    val pluginsDirectory: Path,
    val extensions: Extensions
) {

    fun initialize() {
        extensions.getPlugins().values
            .forEach { (_, plugin) -> plugin.load(this) }

        val plugins = sortPlugins()

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
                .sortedBy { it.name }
                .associateBy({ it.name }, { it.dependencies.toList() })
                .let { toFlattenedDependencyGraph(it) }
                .map { this[it]!! }
        }

    internal fun loadPluginsByServiceFiles() {
        if (Files.notExists(pluginsDirectory)) {
            Files.createDirectories(pluginsDirectory)
        }

        if (!Files.isDirectory(pluginsDirectory)) {
            throw IllegalStateException("The path is not a directory")
        }

        extensions.logger.debug("Plugins directory: ${pluginsDirectory.absolutePathString()}")

        Files.list(pluginsDirectory).use { pluginDirectoryStream ->
            pluginDirectoryStream
                .filter { it.getSimpleName().endsWith(".jar") }
                .filter { isValidJarFile(it.toFile()) }
                .map { it.toUri().toURL() }
                .collect(Collectors.toList())
                .onEach { extensions.logger.debug("Plugin file: $it") }
                .let { URLClassLoader(it.toTypedArray()) }
                .let { ServiceLoader.load(ReposilitePlugin::class.java, it) }
                .onEach { extensions.logger.debug("Plugin class: $it") }
                .forEach { extensions.registerPlugin(it) }
        }
    }

    private fun isValidJarFile(file: File): Boolean =
        Result.attempt { JarFile(file) }
            .map { it.close() }
            .onError { extensions.logger.warn("Invalid JAR file: ${file.absolutePath}") }
            .isOk

}
