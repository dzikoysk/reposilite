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

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Event
import com.reposilite.plugin.api.EventListener
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.api.ReposilitePlugin.ReposilitePluginAccessor
import kotlin.reflect.full.findAnnotation

data class PluginEntry(
    val metadata: Plugin,
    val plugin: ReposilitePlugin
)

class Extensions(private val journalist: Journalist) : Journalist {

    private val plugins: MutableMap<String, PluginEntry> = mutableMapOf()
    private val facades: MutableList<Facade> = mutableListOf()
    private val events: MutableMap<Class<*>, MutableList<EventListener<Event>>> = mutableMapOf()

    fun registerPlugin(plugin: ReposilitePlugin) {
        val pluginEntry = plugin::class.findAnnotation<Plugin>()
            ?.let { PluginEntry(it, plugin) }
            ?.also { ReposilitePluginAccessor.injectExtension(it.plugin, this) }
            ?: throw IllegalStateException("Plugin ${plugin::class} does not have @Plugin annotation")

        val name = pluginEntry.metadata.name

        if (plugins.containsKey(name)) {
            throw IllegalStateException("Plugin $name is already registered")
        }

        plugins[name] = pluginEntry
    }

    fun registerFacade(facade: Facade) {
        facades.add(facade)
    }

    inline fun <reified EVENT : Event> registerEvent(listener: EventListener<EVENT>) =
        registerEvent(EVENT::class.java, listener)

    fun <EVENT : Event> registerEvent(eventClass: Class<in EVENT>, listener: EventListener<EVENT>) {
        val listeners = events.computeIfAbsent(eventClass) { mutableListOf() }
        @Suppress("UNCHECKED_CAST")
        listeners.add(listener as EventListener<Event>)
        listeners.sortedBy { it.priority() }
    }

    fun <E : Event> emitEvent(event: E): E {
        events[event.javaClass]?.forEach { it.onCall(event) }
        return event
    }

    inline fun <reified F : Facade> facade(): F =
        facade(F::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <F : Facade> facade(type: Class<F>): F =
        getFacades().find { type.isInstance(it) }!! as F

    fun getFacades(): Collection<Facade> =
        facades.toList()

    fun getPlugins(): Map<String, PluginEntry> =
        plugins.toMap()

    override fun getLogger(): Logger =
        journalist.logger

}
