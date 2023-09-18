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
import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.api.Credentials
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.journalist.Journalist
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.extensions.response
import com.reposilite.shared.extensions.uri
import com.reposilite.shared.extractFromHeader
import com.reposilite.status.FailureFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.web.api.HttpServerConfigurationEvent
import com.reposilite.web.api.HttpServerStartedEvent
import com.reposilite.web.api.RoutingSetupEvent
import com.reposilite.web.infrastructure.CacheBypassHandler
import com.reposilite.web.infrastructure.EndpointAccessLoggingHandler
import io.javalin.community.routing.dsl.DslExceptionHandler
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.dsl.DslRoutingPlugin
import io.javalin.community.routing.dsl.RoutingDslConfiguration
import io.javalin.community.routing.dsl.RoutingDslFactory
import io.javalin.community.ssl.SSLPlugin
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.http.Header
import io.javalin.http.HttpStatus
import io.javalin.json.JavalinJackson
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.OpenApiPluginConfiguration
import io.javalin.plugin.bundled.SslRedirectPlugin
import kotlin.time.Duration.Companion.minutes
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.thread.ThreadPool
import panda.std.reactive.Reference

internal object JavalinConfiguration {

    internal fun configure(reposilite: Reposilite, webThreadPool: ThreadPool, config: JavalinConfig) {
        val server = Server(webThreadPool)
        config.jetty.server { server }
        reposilite.extensions.emitEvent(HttpServerConfigurationEvent(reposilite, config))

        val localConfiguration = reposilite.extensions.facade<LocalConfiguration>()
        val sharedConfigurationFacade = reposilite.extensions.facade<SharedConfigurationFacade>()
        val webSettings = sharedConfigurationFacade.getDomainSettings<WebSettings>()
        val frontendSettings = sharedConfigurationFacade.getDomainSettings<FrontendSettings>()

        reposilite.extensions.registerEvent { _: HttpServerStartedEvent ->
            server.connectors
                .filterIsInstance<ServerConnector>()
                .forEach { it.idleTimeout = localConfiguration.idleTimeout.get() }
        }

        if (localConfiguration.bypassExternalCache.get()) {
            reposilite.extensions.registerEvent { event: RoutingSetupEvent ->
                event.registerRoutes(EndpointAccessLoggingHandler())
                event.registerRoutes(CacheBypassHandler())
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
            "none" -> config.compression.none()
            "gzip" -> config.compression.gzipOnly()
            "brotli" -> config.compression.brotliOnly()
            else -> error("Unknown compression strategy ${localConfiguration.compressionStrategy.get()}")
        }
    }

    private class ReposiliteDsl(
        val routeFactory: (DslRoute<ContextDsl<*>, Unit>) -> Handler,
        val exceptionRouteFactory: (DslExceptionHandler<ContextDsl<*>, Exception, Unit>) -> ExceptionHandler<Exception>
    ) : RoutingDslFactory<ReposiliteDsl.Configuration, DslRoute<ContextDsl<*>, Unit>, ContextDsl<*>, Unit> {

        open class Configuration : RoutingDslConfiguration<DslRoute<ContextDsl<*>, Unit>, ContextDsl<*>, Unit>()

        override fun createConfiguration(): Configuration =
            Configuration()

        override fun createHandler(route: DslRoute<ContextDsl<*>, Unit>): Handler =
            routeFactory.invoke(route)

        override fun createExceptionHandler(handler: DslExceptionHandler<ContextDsl<*>, Exception, Unit>): ExceptionHandler<Exception> =
            exceptionRouteFactory.invoke(handler)

    }

    private fun configureRoutingPlugin(config: JavalinConfig, reposilite: Reposilite) {
        val extensionManager = reposilite.extensions
        val failureFacade = extensionManager.facade<FailureFacade>()
        val accessTokenFacade = extensionManager.facade<AccessTokenFacade>()
        val authenticationFacade = extensionManager.facade<AuthenticationFacade>()

        fun Context.toDslContext(): ContextDsl<Any> =
            ContextDsl(
                logger = reposilite.logger,
                ctx = this,
                accessTokenFacade = accessTokenFacade,
                authenticationResult = lazy {
                    extractFromHeader(header(Header.AUTHORIZATION))
                        .map { (name, secret) -> Credentials(name, secret) }
                        .flatMap { authenticationFacade.authenticateByCredentials(it) }
                }
            )

        fun ContextDsl<Any>.consumeResponse() {
            response?.also { result ->
                result.onError { reposilite.logger.debug("ERR RESULT | ${ctx.method()} ${ctx.uri()} errored with $it") }
                ctx.response(result)
            }
        }

        val reposiliteDsl = ReposiliteDsl(
            routeFactory = { route ->
                Handler { ctx ->
                    try {
                        val dsl = ctx.toDslContext()
                        route.handler(dsl)
                        dsl.consumeResponse()
                    } catch (throwable: Throwable) {
                        failureFacade.throwException(ctx.uri(), throwable)
                        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    }
                }
            },
            exceptionRouteFactory = { exceptionRoute ->
                ExceptionHandler { exception, ctx ->
                    try {
                        val dsl = ctx.toDslContext()
                        exceptionRoute.invoke(dsl, exception)
                        dsl.consumeResponse()
                    } catch (throwable: Throwable) {
                        failureFacade.throwException(ctx.uri(), throwable)
                        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    }
                }
            }
        )

        val routingPlugin = DslRoutingPlugin(reposiliteDsl)

        extensionManager.emitEvent(RoutingSetupEvent(reposilite))
            .getRoutes()
            .asSequence()
            .flatMap { it.routes() }
            .distinctBy { "${it.method.name}:${it.path}" }
            .toSet()
            .let {
                routingPlugin.routing {
                    addRoutes(it)
                }
            }

        config.plugins.register(routingPlugin)
    }

    private fun configureJsonSerialization(config: JavalinConfig) {
        val objectMapper = ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
        config.jsonMapper(JavalinJackson(objectMapper))
    }

    private fun configureSSL(reposilite: Reposilite, localConfiguration: LocalConfiguration, config: JavalinConfig) {
        if (localConfiguration.sslEnabled.get()) {
            reposilite.logger.info("Enabling SSL connector at ::${localConfiguration.sslPort.get()}")

            val sslPlugin = SSLPlugin { sslConfig ->
                sslConfig.insecure = true
                sslConfig.insecurePort = localConfiguration.port.get()

                sslConfig.secure = true
                sslConfig.securePort = localConfiguration.sslPort.get()

                val keyConfiguration = localConfiguration.keyPath.map {
                    it.replace("\${WORKING_DIRECTORY}", reposilite.parameters.workingDirectory.toAbsolutePath().toString())
                }
                val keyPassword = localConfiguration.keyPassword.get()

                when {
                    keyConfiguration.endsWith(".pem") -> {
                        val (certPath, keyPath) = keyConfiguration.split(" ")
                        sslConfig.pemFromPath(certPath, keyPath, keyPassword)
                    }
                    keyConfiguration.endsWith(".jks") -> sslConfig.keystoreFromPath(keyConfiguration, keyPassword)
                    else -> throw IllegalArgumentException("Provided key extension is not supported.")
                }

                sslConfig.configConnectors { it.idleTimeout = localConfiguration.idleTimeout.get() }
                sslConfig.sniHostCheck = false
            }

            config.plugins.register(sslPlugin)
        }

        if (localConfiguration.enforceSsl.get()) {
            config.plugins.register(
                SslRedirectPlugin(
                    redirectOnLocalhost = true,
                    sslPort = localConfiguration.sslPort.get(),
                )
            )
        }
    }

    private fun configureCors(config: JavalinConfig) {
        config.plugins.enableCors {
            it.add { cfg ->
                cfg.anyHost()
            }
        }
    }

    private fun configureOpenApi(config: JavalinConfig, frontendSettings: FrontendSettings) {
        config.plugins.register(
            OpenApiPlugin(
                OpenApiPluginConfiguration()
                    .withDefinitionConfiguration { _, configuration ->
                        configuration.withOpenApiInfo {
                            it.title = frontendSettings.title
                            it.description = frontendSettings.description
                            it.version = VERSION
                        }
                    }
            )
        )
    }

    private fun configureDebug(journalist: Journalist, localConfiguration: LocalConfiguration, config: JavalinConfig) {
        if (localConfiguration.debugEnabled.get()) {
            // config.requestCacheSize = FilesUtils.displaySizeToBytesCount(System.getProperty("reposilite.requestCacheSize", "8MB"));
            // Reposilite.getLogger().debug("requestCacheSize set to " + config.requestCacheSize + " bytes");
            journalist.logger.info("Debug enabled")
            config.plugins.enableDevLogging()
        }
    }

}
