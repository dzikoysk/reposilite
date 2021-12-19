package com.reposilite.plugin

import com.reposilite.ReposiliteParameters
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Event
import com.reposilite.plugin.api.EventListener
import com.reposilite.plugin.api.Facade
import com.reposilite.settings.api.LocalConfiguration
import org.jetbrains.exposed.sql.Database

class ExtensionsManagement(
    private val journalist: Journalist,
    val parameters: ReposiliteParameters,
    val localConfiguration: LocalConfiguration,
    val database: Database
) : Journalist {

    private val plugins: MutableList<ReposilitePlugin> = mutableListOf()
    private val facades: MutableList<Facade> = mutableListOf()
    private val events: MutableMap<Class<*>, MutableList<EventListener<Event>>> = mutableMapOf()

    fun registerPlugin(plugin: ReposilitePlugin) {
        plugins.add(plugin)
    }

    inline fun <reified EVENT : Event> registerEvent(listener: EventListener<EVENT>) =
        registerEvent(EVENT::class.java, listener)

    fun <EVENT : Event> registerEvent(eventClass: Class<in EVENT>, listener: EventListener<EVENT>) {
        val listeners = events.computeIfAbsent(eventClass) { mutableListOf() }
        @Suppress("UNCHECKED_CAST")
        listeners.add(listener as EventListener<Event>)
        listeners.sortedBy { it.priority() }
    }

    fun <E : Event> notifyListeners(event: E): E {
        events[event.javaClass]?.forEach { it.onCall(event) }
        return event
    }

    inline fun <reified F : Facade> facade(): F =
        getFacades().find { it is F } as F

    fun getFacades(): Collection<Facade> =
        facades

    inline fun <reified P : ReposilitePlugin> plugin(): P =
        getPlugins().find { it is P } as P

    fun getPlugins(): Collection<ReposilitePlugin> =
        plugins

    override fun getLogger(): Logger =
        journalist.logger

}