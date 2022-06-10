/*
 * Copyright (c) 2022 dzikoysk
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

import com.reposilite.Reposilite
import com.reposilite.ReposiliteObjectMapper
import com.reposilite.VERSION
import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.api.Credentials
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.journalist.Journalist
import com.reposilite.shared.ContextDsl
import com.reposilite.status.FailureFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.web.api.HttpServerConfigurationEvent
import com.reposilite.web.api.RoutingSetupEvent
import com.reposilite.web.http.extractFromHeaders
import com.reposilite.web.http.response
import com.reposilite.web.http.uri
import com.reposilite.web.routing.RoutingPlugin
import io.javalin.core.JavalinConfig
import io.javalin.core.compression.CompressionStrategy
import io.javalin.openapi.plugin.OpenApiConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.plugin.json.JavalinJackson
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.util.thread.ThreadPool
import panda.std.reactive.Reference

internal object JavalinConfiguration {

    internal fun configure(reposilite: Reposilite, webThreadPool: ThreadPool, config: JavalinConfig) {
        val server = Server(webThreadPool)
        config.server { server }
        reposilite.extensions.emitEvent(HttpServerConfigurationEvent(reposilite, config))

        val localConfiguration = reposilite.extensions.facade<LocalConfiguration>()
        val sharedConfigurationFacade = reposilite.extensions.facade<SharedConfigurationFacade>()
        val webSettings = sharedConfigurationFacade.getDomainSettings<WebSettings>()
        val frontendSettings = sharedConfigurationFacade.getDomainSettings<FrontendSettings>()

        configureJavalin(config, localConfiguration, webSettings)
        configureJsonSerialization(config)
        configureSSL(reposilite, localConfiguration, config, server)
        configureCors(config)
        configureOpenApi(config, frontendSettings.get())
        configureDebug(reposilite, localConfiguration, config)
        configureReactiveRoutingPlugin(config, reposilite)
    }

    private fun configureJavalin(config: JavalinConfig, localConfiguration: LocalConfiguration, webSettings: Reference<WebSettings>) {
        config.showJavalinBanner = false
        config.asyncRequestTimeout = 1000L * 60 * 60 * 10 // 10min

        config.contextResolvers {
            it.ip = { ctx -> ctx.header(webSettings.get().forwardedIp) ?: ctx.req.remoteAddr }
        }

        when (localConfiguration.compressionStrategy.get().lowercase()) {
            "none" -> config.compressionStrategy(CompressionStrategy.NONE)
            "gzip" -> config.compressionStrategy(CompressionStrategy.GZIP)
            else -> throw IllegalStateException("Unknown compression strategy ${localConfiguration.compressionStrategy.get()}")
        }
    }

    private fun configureReactiveRoutingPlugin(config: JavalinConfig, reposilite: Reposilite) {
        val extensionManager = reposilite.extensions
        val failureFacade = extensionManager.facade<FailureFacade>()
        val accessTokenFacade = extensionManager.facade<AccessTokenFacade>()
        val authenticationFacade = extensionManager.facade<AuthenticationFacade>()

        val plugin = RoutingPlugin<ContextDsl<*>, Unit>(
            handler = { ctx, route ->
                try {
                    val dsl = ContextDsl<Any>(
                        reposilite.logger,
                        ctx,
                        accessTokenFacade,
                        lazy {
                            extractFromHeaders(ctx.headerMap())
                                .map { (name, secret) -> Credentials(name, secret) }
                                .flatMap { authenticationFacade.authenticateByCredentials(it) }
                        }
                    )
                    route.handler(dsl)
                    dsl.response?.also { ctx.response(it) }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                    failureFacade.throwException(ctx.uri(), throwable)
                }
            }
        )

        extensionManager.emitEvent(RoutingSetupEvent(reposilite))
            .getRoutes().asSequence()
            .flatMap { it.routes }
            .distinctBy { it.methods.joinToString(";") + ":" + it.path }
            .toSet()
            .let { plugin.registerRoutes(it) }

        config.registerPlugin(plugin)
    }

    private fun configureJsonSerialization(config: JavalinConfig) {
        val objectMapper = ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
        config.jsonMapper(JavalinJackson(objectMapper))
    }

    private fun configureSSL(reposilite: Reposilite, localConfiguration: LocalConfiguration, config: JavalinConfig, server: Server) {
        if (localConfiguration.sslEnabled.get()) {
            reposilite.logger.info("Enabling SSL connector at ::" + localConfiguration.sslPort.get())

            val sslContextFactory = SslContextFactory.Server()
            sslContextFactory.keyStorePath = localConfiguration.keyStorePath.get().replace("\${WORKING_DIRECTORY}", reposilite.parameters.workingDirectory.toAbsolutePath().toString())
            sslContextFactory.setKeyStorePassword(localConfiguration.keyStorePassword.get())

            val sslConnector = ServerConnector(server, sslContextFactory)
            sslConnector.port = localConfiguration.sslPort.get()
            server.addConnector(sslConnector)

            if (!localConfiguration.enforceSsl.get()) {
                val standardConnector = ServerConnector(server)
                standardConnector.port = localConfiguration.port.get()
                server.addConnector(standardConnector)
            }
        }

        config.enforceSsl = localConfiguration.enforceSsl.get()
    }

    private fun configureCors(config: JavalinConfig) {
        config.enableCorsForAllOrigins()
    }

    private fun configureOpenApi(config: JavalinConfig, frontendSettings: FrontendSettings) {
        val openApiConfiguration = OpenApiConfiguration()
        openApiConfiguration.title = frontendSettings.title
        openApiConfiguration.description = frontendSettings.description
        openApiConfiguration.version = VERSION
        config.registerPlugin(OpenApiPlugin(openApiConfiguration))
    }

    private fun configureDebug(journalist: Journalist, localConfiguration: LocalConfiguration, config: JavalinConfig) {
        if (localConfiguration.debugEnabled.get()) {
            // config.requestCacheSize = FilesUtils.displaySizeToBytesCount(System.getProperty("reposilite.requestCacheSize", "8MB"));
            // Reposilite.getLogger().debug("requestCacheSize set to " + config.requestCacheSize + " bytes");
            journalist.logger.info("Debug enabled")
            config.enableDevLogging()
        }
    }

}
