package com.reposilite.web.application

import com.reposilite.web.WebServer
import com.reposilite.web.infrastructure.JavalinWebServer

object WebConfiguration {

    fun createWebServer(): WebServer =
        JavalinWebServer()

}