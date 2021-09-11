package com.reposilite.web

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.reposilite.Reposilite
import com.reposilite.VERSION
import com.reposilite.config.Configuration
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

    internal fun configure(reposilite: Reposilite, threadPool: ThreadPool, configuration: Configuration, config: JavalinConfig) {
        val server = Server(threadPool)
        config.server { server }

        configureJavalin(config, configuration)
        configureJsonSerialization(config)
        configureSSL(reposilite, configuration, config, server)
        configureCors(config)
        configureOpenApi(configuration, config)
        configureDebug(reposilite, configuration, config)
    }

    private fun configureJavalin(config: JavalinConfig, configuration: Configuration) {
        config.showJavalinBanner = false
        config.asyncRequestTimeout = 1000L * 60 * 60 * 10 // 10min
        config.contextResolvers {
            it.ip = { ctx -> ctx.header(configuration.forwardedIp) ?: ctx.req.remoteAddr }
        }
    }

    private fun configureJsonSerialization(config: JavalinConfig) {
        val objectMapper = JsonMapper.builder()
            .addModule(JavaTimeModule())
            .build()
            .setSerializationInclusion(Include.NON_NULL)

        config.jsonMapper(JavalinJackson(objectMapper))
    }

    private fun configureSSL(reposilite: Reposilite, configuration: Configuration, config: JavalinConfig, server: Server) {
        if (configuration.sslEnabled) {
            reposilite.logger.info("Enabling SSL connector at ::" + configuration.sslPort)

            val sslContextFactory: SslContextFactory = SslContextFactory.Server()
            sslContextFactory.keyStorePath = configuration.keyStorePath.replace("\${WORKING_DIRECTORY}", reposilite.parameters.workingDirectory.toAbsolutePath().toString())
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
    }

    private fun configureCors(config: JavalinConfig) {
        config.enableCorsForAllOrigins()
    }

    private fun configureOpenApi(configuration: Configuration, config: JavalinConfig) {
        if (configuration.swagger) {
            val openApiConfiguration = OpenApiConfiguration()
            openApiConfiguration.title = configuration.title
            openApiConfiguration.description = configuration.description
            openApiConfiguration.version = VERSION
            config.registerPlugin(OpenApiPlugin(openApiConfiguration))

            val swaggerConfiguration = SwaggerConfiguration()
            swaggerConfiguration.title = openApiConfiguration.title
            config.registerPlugin(SwaggerPlugin(swaggerConfiguration))
        }
    }

    private fun configureDebug(reposilite: Reposilite, configuration: Configuration, config: JavalinConfig) {
        if (configuration.debugEnabled) {
            // config.requestCacheSize = FilesUtils.displaySizeToBytesCount(System.getProperty("reposilite.requestCacheSize", "8MB"));
            // Reposilite.getLogger().debug("requestCacheSize set to " + config.requestCacheSize + " bytes");
            reposilite.logger.info("Debug enabled")
            config.enableDevLogging()
        }
    }

}