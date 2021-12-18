package com.reposilite.web.api

import com.reposilite.Reposilite
import io.javalin.Javalin

class HttpServerInitializationEvent(val reposilite: Reposilite, val javalin: Javalin)