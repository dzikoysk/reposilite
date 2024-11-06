/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.plugin.groovy

import com.reposilite.plugin.PluginLoader
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import groovy.lang.GroovyClassLoader
import kotlin.io.path.extension
import kotlin.io.path.useDirectoryEntries

@Plugin(name = "groovy")
class GroovyPlugin : ReposilitePlugin() {

    override fun load(loader: PluginLoader) {
        val groovyClassLoader = GroovyClassLoader(this::class.java.classLoader)

        loader.pluginsDirectory.useDirectoryEntries { pluginDirectoryStream ->
            pluginDirectoryStream
                .filter { it.extension == "groovy" }
                .map { groovyClassLoader.parseClass(it.toFile()) }
                .forEach { extensions().registerPlugin(it.getConstructor().newInstance() as ReposilitePlugin) }
        }
    }

    override fun initialize(): Facade? =
        null

}