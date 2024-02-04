/*
 * Copyright (c) 2023 dzikoysk
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
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.journalist.Journalist
import com.reposilite.web.api.HttpServerConfigurationEvent
import com.reposilite.web.api.HttpServerStartedEvent
import com.reposilite.web.api.RoutingSetupEvent
import com.reposilite.web.infrastructure.ApiCacheBypassHandler
import com.reposilite.web.infrastructure.EndpointAccessLoggingHandler
import com.reposilite.web.infrastructure.createReposiliteDsl
import io.javalin.community.ssl.SslPlugin
import io.javalin.config.JavalinConfig
import io.javalin.json.JavalinJackson
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.plugin.bundled.SslRedirectPlugin
import kotlin.time.Duration.Companion.minutes
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.thread.ThreadPool
import panda.std.reactive.Reference

internal object JavalinConfiguration {

    internal fun configure(reposilite: Reposilite, webThreadPool: ThreadPool, config: JavalinConfig) {
        val server = Server(webThreadPool)
        config.pvt.jetty.server = server
        reposilite.extensions.emitEvent(HttpServerConfigurationEvent(reposilite, config))

        val localConfiguration = reposilite.extensions.facade<LocalConfiguration>()
        val sharedConfigurationFacade = reposilite.extensions.facade<SharedConfigurationFacade>()
        val webSettings = sharedConfigurationFacade.getDomainSettings<WebSettings>()
        val frontendSettings = sharedConfigurationFacade.getDomainSettings<FrontendSettings>()

        reposilite.extensions.registerEvent { _: HttpServerStartedEvent ->
            server.connectors.filterIsInstance<ServerConnector>().forEach { it.idleTimeout = localConfiguration.idleTimeout.get() }
        }

        if (localConfiguration.bypassExternalCache.get()) {
            reposilite.extensions.registerEvent { event: RoutingSetupEvent ->
                event.registerRoutes(EndpointAccessLoggingHandler())
                event.registerRoutes(ApiCacheBypassHandler())
                reposilite.logger.debug("CacheBypassHandler has been registered")
            }
        }

        configureJavalin(config, localConfiguration, webSettings)
        configureJsonSerialization(config)
        configureSSL(reposilite, localConfiguration, config)
        configureCors(config)
        configureOpenApi(config, frontendSettings.get())
        configureDebug(reposilite, localConfiguration, config)
        configureRoutingPlugin(config, reposilite)
    }

    private fun configureJavalin(config: JavalinConfig, localConfiguration: LocalConfiguration, webSettings: Reference<WebSettings>) {
        config.showJavalinBanner = false
        config.http.asyncTimeout = 10.minutes.inWholeMilliseconds
        config.contextResolver.ip = { it.header(webSettings.get().forwardedIp) ?: it.req().remoteAddr }

        System.getProperty("javalin.maxRequestSize")?.let {
            config.http.maxRequestSize = it.toLong()
        }

        when (localConfiguration.compressionStrategy.get().lowercase()) {
            "none" -> config.http.disableCompression()
            "gzip" -> config.http.gzipOnlyCompression()
            "brotli" -> config.http.brotliOnlyCompression()
            else -> error("Unknown compression strategy ${localConfiguration.compressionStrategy.get()}")
        }
    }

    private fun configureRoutingPlugin(config: JavalinConfig, reposilite: Reposilite) {
        val extensionManager = reposilite.extensions

        val reposiliteDsl = createReposiliteDsl(
            journalist = reposilite.logger,
            failureFacade = extensionManager.facade(),
            accessTokenFacade = extensionManager.facade(),
            authenticationFacade = extensionManager.facade()
        )

        extensionManager.emitEvent(RoutingSetupEvent(reposilite)).getRoutes().asSequence().flatMap { it.routes() }.distinctBy { "${it.method.name}:${it.path}" }
            .toSet().let { route ->
                config.router.mount(reposiliteDsl) {
                    it.routes(route)
                }
            }
    }

    private fun configureJsonSerialization(config: JavalinConfig) {
        val objectMapper = ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
        config.jsonMapper(JavalinJackson(objectMapper))
    }

    private fun configureSSL(reposilite: Reposilite, localConfiguration: LocalConfiguration, config: JavalinConfig) {
        if (localConfiguration.sslEnabled.get()) {
            reposilite.logger.info("Enabling SSL connector at ::${localConfiguration.sslPort.get()}")

            config.registerPlugin(SslPlugin {
                it.insecure = true
                it.insecurePort = localConfiguration.port.get()

                it.secure = true
                it.securePort = localConfiguration.sslPort.get()

                val keyConfiguration = localConfiguration.keyPath.map { path ->
                    path.replace("\${WORKING_DIRECTORY}", reposilite.parameters.workingDirectory.toAbsolutePath().toString())
                }
                val keyPassword = localConfiguration.keyPassword.get()

                when {
                    keyConfiguration.endsWith(".pem") -> {
                        val (certPath, keyPath) = keyConfiguration.split(" ")
                        it.pemFromPath(certPath, keyPath, keyPassword)
                    }
                    keyConfiguration.endsWith(".jks") -> it.keystoreFromPath(keyConfiguration, keyPassword)
                    else -> throw IllegalArgumentException("Provided key extension is not supported.")
                }

                it.configConnectors { connector -> connector.idleTimeout = localConfiguration.idleTimeout.get() }
                it.sniHostCheck = false
            })
        }

        if (localConfiguration.enforceSsl.get()) {
            config.registerPlugin(SslRedirectPlugin {
                it.redirectOnLocalhost = true
                it.sslPort = localConfiguration.sslPort.get()
            })
        }
    }

    private fun configureCors(config: JavalinConfig) {
        config.bundledPlugins.enableCors {
            it.addRule { cfg ->
                cfg.anyHost()
            }
        }
    }

    private fun configureOpenApi(config: JavalinConfig, frontendSettings: FrontendSettings) {
        config.registerPlugin(OpenApiPlugin {
            it.withDefinitionConfiguration { _, configuration ->
                configuration.withOpenApiInfo { info ->
                    info.title = frontendSettings.title
                    info.description = frontendSettings.description
                    info.version = VERSION
                }
            }
        })
    }

    private fun configureDebug(journalist: Journalist, localConfiguration: LocalConfiguration, config: JavalinConfig) {
        if (localConfiguration.debugEnabled.get()) {
            // config.requestCacheSize = FilesUtils.displaySizeToBytesCount(System.getProperty("reposilite.requestCacheSize", "8MB"));
            // Reposilite.getLogger().debug("requestCacheSize set to " + config.requestCacheSize + " bytes");
            journalist.logger.info("Debug enabled")
            config.bundledPlugins.enableDevLogging()
        }
    }

}
