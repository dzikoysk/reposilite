package com.reposilite.plugin

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Event
import com.reposilite.plugin.api.EventListener
import com.reposilite.plugin.api.Facade

abstract class ReposilitePlugin : Journalist {

    lateinit var extensions: ExtensionsManagement // injected

    abstract fun initialize(): Facade?

    inline fun <reified EVENT : Event> event(listener: EventListener<EVENT>) =
        extensions.registerEvent(listener)

    internal inline fun <reified F : Facade> facade(): F =
        extensions.findFacade()!!

    internal inline fun <reified P : ReposilitePlugin> plugin(): P =
        extensions.findPlugin()!!

    override fun getLogger(): Logger =
        extensions.logger

}