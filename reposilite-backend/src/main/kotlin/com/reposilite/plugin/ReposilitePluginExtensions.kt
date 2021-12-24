package com.reposilite.plugin

import com.reposilite.plugin.api.Event
import com.reposilite.plugin.api.EventListener
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.ReposilitePlugin

inline fun <reified EVENT : Event> ReposilitePlugin.event(listener: EventListener<EVENT>) =
    extensions().registerEvent(listener)

inline fun <reified F : Facade> ReposilitePlugin.facade(): F =
    extensions().facade()