/*
 * Copyright (c) 2021 dzikoysk
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

package com.reposilite.web

import com.reposilite.Reposilite
import com.reposilite.config.Configuration
import com.reposilite.shared.TimeUtils
import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.eclipse.jetty.io.EofException
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.util.thread.ThreadPool

class JavalinWebServer {

    private val servlet = false
    private var javalin: Javalin? = null
    private var webThreadPool: QueuedThreadPool? = null

    fun start(reposilite: Reposilite) =
        runWithDisabledLogging {
            this.webThreadPool = QueuedThreadPool(reposilite.configuration.webThreadPool, 2)
            webThreadPool?.start()

            this.javalin = createJavalin(reposilite, reposilite.configuration, webThreadPool!!, reposilite.ioDispatcher ?: Dispatchers.Default)
                .exception(EofException::class.java) { _, _ -> reposilite.logger.warn("Client closed connection") }
                .events { listener ->
                    listener.serverStopping { reposilite.logger.info("Server stopping...") }
                    listener.serverStopped { reposilite.logger.info("Bye! Uptime: " + TimeUtils.getPrettyUptimeInMinutes(reposilite.statusFacade.startTime)) }
                }
                .also {
                    reposilite.webs.forEach { web -> web.javalin(reposilite, it) }
                }

            if (!servlet) {
                javalin!!.start(reposilite.parameters.hostname, reposilite.parameters.port)
            }
        }

    private fun createJavalin(reposilite: Reposilite, configuration: Configuration, webThreadPool: ThreadPool, dispatcher: CoroutineDispatcher): Javalin =
        if (servlet)
            Javalin.createStandalone { configureServer(reposilite, configuration, webThreadPool, dispatcher, it) }
        else
            Javalin.create { configureServer(reposilite, configuration, webThreadPool, dispatcher, it) }

    private fun configureServer(reposilite: Reposilite, configuration: Configuration, webThreadPool: ThreadPool, dispatcher: CoroutineDispatcher, serverConfig: JavalinConfig) {
        WebServerConfiguration.configure(reposilite, webThreadPool, configuration, serverConfig)
        serverConfig.registerPlugin(createReactiveRouting(reposilite, dispatcher))
    }

    fun stop() {
        webThreadPool?.stop()
        javalin?.stop()
    }

    fun isAlive(): Boolean =
        javalin?.jettyServer()?.server()?.isStarted ?: false

}