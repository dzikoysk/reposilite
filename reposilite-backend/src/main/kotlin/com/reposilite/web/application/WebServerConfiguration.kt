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

package com.reposilite.web.application

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.reposilite.Reposilite
import com.reposilite.VERSION
import com.reposilite.journalist.Journalist
import com.reposilite.settings.LocalConfiguration
import com.reposilite.settings.SharedConfiguration
import io.javalin.core.JavalinConfig
import io.javalin.openapi.plugin.OpenApiConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import io.javalin.plugin.json.JavalinJackson
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.util.thread.ThreadPool

internal object WebServerConfiguration {

    internal fun configure(reposilite: Reposilite, webThreadPool: ThreadPool, config: JavalinConfig) {
        val server = Server(webThreadPool)
        config.server { server }

        val localConfiguration = reposilite.localConfiguration
        val sharedConfiguration = reposilite.sharedConfiguration

        configureJavalin(config, sharedConfiguration)
        configureJsonSerialization(config)
        configureSSL(reposilite, localConfiguration, config, server)
        configureCors(config)
        configureOpenApi(sharedConfiguration, config)
        configureDebug(reposilite, localConfiguration, config)
    }

    private fun configureJavalin(config: JavalinConfig, sharedConfiguration: SharedConfiguration) {
        config.showJavalinBanner = false
        config.asyncRequestTimeout = 1000L * 60 * 60 * 10 // 10min
        config.contextResolvers {
            it.ip = { ctx -> ctx.header(sharedConfiguration.forwardedIp) ?: ctx.req.remoteAddr }
        }
    }

    private fun configureJsonSerialization(config: JavalinConfig) {
        val objectMapper = JsonMapper.builder()
            .addModule(JavaTimeModule())
            .build()
            .setSerializationInclusion(Include.NON_NULL)

        config.jsonMapper(JavalinJackson(objectMapper))
    }

    private fun configureSSL(reposilite: Reposilite, localConfiguration: LocalConfiguration, config: JavalinConfig, server: Server) {
        if (localConfiguration.sslEnabled) {
            reposilite.logger.info("Enabling SSL connector at ::" + localConfiguration.sslPort)

            val sslContextFactory: SslContextFactory = SslContextFactory.Server()
            sslContextFactory.keyStorePath = localConfiguration.keyStorePath.replace("\${WORKING_DIRECTORY}", reposilite.parameters.workingDirectory.toAbsolutePath().toString())
            sslContextFactory.setKeyStorePassword(localConfiguration.keyStorePassword)

            val sslConnector = ServerConnector(server, sslContextFactory)
            sslConnector.port = localConfiguration.sslPort
            server.addConnector(sslConnector)

            if (!localConfiguration.enforceSsl) {
                val standardConnector = ServerConnector(server)
                standardConnector.port = localConfiguration.port
                server.addConnector(standardConnector)
            }
        }

        config.enforceSsl = localConfiguration.enforceSsl
    }

    private fun configureCors(config: JavalinConfig) {
        config.enableCorsForAllOrigins()
    }

    private fun configureOpenApi(sharedConfiguration: SharedConfiguration, config: JavalinConfig) {
        if (sharedConfiguration.swagger) {
            val openApiConfiguration = OpenApiConfiguration()
            openApiConfiguration.title = sharedConfiguration.title
            openApiConfiguration.description = sharedConfiguration.description
            openApiConfiguration.version = VERSION
            config.registerPlugin(OpenApiPlugin(openApiConfiguration))

            val swaggerConfiguration = SwaggerConfiguration()
            swaggerConfiguration.title = openApiConfiguration.title
            config.registerPlugin(SwaggerPlugin(swaggerConfiguration))
        }
    }

    private fun configureDebug(journalist: Journalist, localConfiguration: LocalConfiguration, config: JavalinConfig) {
        if (localConfiguration.debugEnabled) {
            // config.requestCacheSize = FilesUtils.displaySizeToBytesCount(System.getProperty("reposilite.requestCacheSize", "8MB"));
            // Reposilite.getLogger().debug("requestCacheSize set to " + config.requestCacheSize + " bytes");
            journalist.logger.info("Debug enabled")
            config.enableDevLogging()
        }
    }

}