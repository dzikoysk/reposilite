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
package org.panda_lang.reposilite

import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import io.javalin.core.event.EventListener
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.Info
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.panda_lang.reposilite.config.Configuration

class ReposiliteHttpServer internal constructor(private val reposilite: Reposilite, private val servlet: Boolean) {

    var javalin: Javalin? = null

    fun start(configuration: Configuration) {
        javalin = create(configuration)

        // install routes here
        javalin!!.events { event: EventListener ->
            event.serverStopping {}
            event.serverStopped { reposilite.logger.info("Bye! Uptime: " + reposilite.getPrettyUptime()) }
        }

        if (!servlet) {
            javalin!!.start(configuration.hostname, configuration.port)
        }
    }

    private fun create(configuration: Configuration): Javalin {
        return if (servlet) Javalin.createStandalone { config: JavalinConfig ->
            configure(
                configuration,
                config
            )
        }
        else Javalin.create { config: JavalinConfig -> configure(configuration, config) }
    }

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
            val applicationInfo = Info()
                .description(ReposiliteConstants.NAME)
                .version(ReposiliteConstants.VERSION)

            val swaggerOptions = SwaggerOptions("/swagger")
                .title("Reposilite API documentation")

            val options = OpenApiOptions(applicationInfo)
                .path("/swagger-docs") // .reDoc(new ReDocOptions("/redoc"))
                .swagger(swaggerOptions)

            config.registerPlugin(OpenApiPlugin(options))
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