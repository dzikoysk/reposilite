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
package org.panda_lang.reposilite.web.application

import com.dzikoysk.openapi.javalin.OpenApiConfiguration
import com.dzikoysk.openapi.javalin.OpenApiPlugin
import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.panda_lang.reposilite.Reposilite
import org.panda_lang.reposilite.ReposiliteWebConfiguration
import org.panda_lang.reposilite.VERSION
import org.panda_lang.reposilite.config.Configuration
import org.panda_lang.reposilite.web.RouteMethod.AFTER
import org.panda_lang.reposilite.web.RouteMethod.BEFORE
import org.panda_lang.reposilite.web.RouteMethod.DELETE
import org.panda_lang.reposilite.web.RouteMethod.GET
import org.panda_lang.reposilite.web.RouteMethod.HEAD
import org.panda_lang.reposilite.web.RouteMethod.POST
import org.panda_lang.reposilite.web.RouteMethod.PUT

internal class HttpServerConfiguration internal constructor(
    private val reposilite: Reposilite,
    private val servlet: Boolean
) {

    var javalin: Javalin? = null
        private set

    fun start(configuration: Configuration) {
        val javalin = create(configuration)
            .events { listener ->
                listener.serverStopping { reposilite.logger.info("Server stopping...") }
                listener.serverStopped { reposilite.logger.info("Bye! Uptime: " + reposilite.getPrettyUptime()) }
            }
            .also { this.javalin = it }

        ReposiliteWebConfiguration.routing(reposilite).forEach { handler ->
            handler.methods.forEach { method ->
                when (method) {
                    HEAD -> javalin.head(handler.route, handler)
                    GET -> javalin.get(handler.route, handler)
                    PUT -> javalin.put(handler.route, handler)
                    POST -> javalin.post(handler.route, handler)
                    DELETE -> javalin.delete(handler.route, handler)
                    AFTER -> javalin.after(handler.route, handler)
                    BEFORE -> javalin.before(handler.route, handler)
                }
            }
        }

        if (!servlet) {
            javalin?.start(configuration.hostname, configuration.port)
        }
    }

    private fun create(configuration: Configuration): Javalin =
        if (servlet)
            Javalin.createStandalone { configure(configuration, it) }
        else
            Javalin.create { configure(configuration, it) }

    private fun configure(configuration: Configuration, config: JavalinConfig) {
        val server = Server()

        if (configuration.sslEnabled) {
            reposilite.logger.info("Enabling SSL connector at ::" + configuration.sslPort)

            val sslContextFactory: SslContextFactory = SslContextFactory.Server()
            sslContextFactory.keyStorePath =
                configuration.keyStorePath.replace("\${WORKING_DIRECTORY}", reposilite.workingDirectory.toAbsolutePath().toString())
            sslContextFactory.setKeyStorePassword(configuration.keyStorePassword)

            val sslConnector = ServerConnector(server, sslContextFactory)
            sslConnector.port = configuration.sslPort
            server.addConnector(sslConnector)

            if (!configuration.enforceSsl) {
                val standardConnector = ServerConnector(server)
                standardConnector.port = configuration.port
                server.addConnector(standardConnector)
            }
        }

        config.enforceSsl = configuration.enforceSsl
        config.enableCorsForAllOrigins()
        config.showJavalinBanner = false

        if (configuration.swagger) {
            val openApiConfiguration = OpenApiConfiguration()
            openApiConfiguration.title = configuration.title
            openApiConfiguration.description = configuration.description
            openApiConfiguration.version = VERSION
            config.registerPlugin(OpenApiPlugin(openApiConfiguration))
        }

        if (configuration.debugEnabled) {
            // config.requestCacheSize = FilesUtils.displaySizeToBytesCount(System.getProperty("reposilite.requestCacheSize", "8MB"));
            // Reposilite.getLogger().debug("requestCacheSize set to " + config.requestCacheSize + " bytes");
            reposilite.logger.info("Debug enabled")
            config.enableDevLogging()
        }

        config.server { server }
    }

    fun stop(): Javalin? =
        javalin?.stop()

    fun isAlive(): Boolean =
        javalin?.server()?.server()?.isStarted ?: false

}