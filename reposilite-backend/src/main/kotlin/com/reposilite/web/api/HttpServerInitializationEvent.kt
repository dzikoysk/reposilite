package com.reposilite.web.api

import com.reposilite.Reposilite
import com.reposilite.plugin.api.Event
import io.javalin.Javalin

class HttpServerInitializationEvent(val reposilite: Reposilite, val javalin: Javalin) : Event

class HttpServerStoppedEvent() : Event