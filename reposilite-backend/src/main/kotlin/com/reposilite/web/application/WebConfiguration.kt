package com.reposilite.web.application

import com.reposilite.web.WebServer
import com.reposilite.web.infrastructure.JavalinWebServer
import org.eclipse.jetty.util.thread.ThreadPool

object WebConfiguration {

    fun createWebServer(threadPool: ThreadPool): WebServer =
        JavalinWebServer(threadPool)

}