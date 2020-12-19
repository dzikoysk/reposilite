/*
 * Copyright (c) 2020 Dzikoysk
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

package org.panda_lang.reposilite;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.core.JavalinServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.panda_lang.reposilite.auth.AuthEndpoint;
import org.panda_lang.reposilite.auth.PostAuthHandler;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.console.CliController;
import org.panda_lang.reposilite.console.RemoteExecutionEndpoint;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.frontend.FrontendController;
import org.panda_lang.reposilite.repository.DeployEndpoint;
import org.panda_lang.reposilite.repository.LookupApiEndpoint;
import org.panda_lang.reposilite.repository.LookupController;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.function.Option;

public final class ReposiliteHttpServer {

    private final Reposilite reposilite;
    private final boolean servlet;
    private Javalin javalin;

    ReposiliteHttpServer(Reposilite reposilite, boolean servlet) {
        this.reposilite = reposilite;
        this.servlet = servlet;
    }

    void start(Configuration configuration, Runnable onStart) {
        FailureService failureService = reposilite.getFailureService();
        DeployEndpoint deployEndpoint = new DeployEndpoint(reposilite.getContextFactory(), reposilite.getDeployService());

        LookupController lookupController = new LookupController(
                configuration.proxied.size() > 0,
                reposilite.getContextFactory(),
                reposilite.getFrontendService(),
                reposilite.getLookupService(),
                reposilite.getProxyService(),
                reposilite.getFailureService());

        LookupApiEndpoint lookupApiEndpoint = new LookupApiEndpoint(
                configuration.rewritePathsEnabled,
                reposilite.getContextFactory(),
                reposilite.getRepositoryAuthenticator(),
                reposilite.getRepositoryService(),
                reposilite.getLookupService());

        CliController cliController = new CliController(
                reposilite.getContextFactory(),
                reposilite.getExecutor(),
                reposilite.getAuthenticator(),
                reposilite.getConsole());

        this.javalin = create(configuration)
                .before(ctx -> reposilite.getStatsService().record(ctx.req.getRequestURI()))
                .get("/js/app.js", new FrontendController(reposilite))
                .get("/api/auth", new AuthEndpoint(reposilite.getAuthService()))
                .post("/api/execute", new RemoteExecutionEndpoint(reposilite.getAuthenticator(), reposilite.getContextFactory(), reposilite.getConsole()))
                .ws("/api/cli", cliController)
                .get("/api", lookupApiEndpoint)
                .get("/api/*", lookupApiEndpoint)
                .get("/*", lookupController)
                .head("/*", lookupController)
                .put("/*", deployEndpoint)
                .post("/*", deployEndpoint)
                .after("/*", new PostAuthHandler())
                .exception(Exception.class, (exception, ctx) -> failureService.throwException(ctx.req.getRequestURI(), exception));

        if (!servlet) {
            javalin.start(configuration.hostname, configuration.port);
            onStart.run();
        }
    }

    private Javalin create(Configuration configuration) {
        return servlet
                ? Javalin.createStandalone(config -> configure(configuration, config))
                : Javalin.create(config -> configure(configuration, config));
    }

    private void configure(Configuration configuration, JavalinConfig config) {
        Server server = new Server();

        if (configuration.sslEnabled) {
            Reposilite.getLogger().info("Enabling SSL connector at ::" + configuration.sslPort);

            SslContextFactory sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(configuration.keyStorePath.replace("${WORKING_DIRECTORY}", reposilite.getWorkingDirectory().getAbsolutePath()));
            sslContextFactory.setKeyStorePassword(configuration.keyStorePassword);

            ServerConnector sslConnector = new ServerConnector(server, sslContextFactory);
            sslConnector.setPort(configuration.sslPort);
            server.addConnector(sslConnector);

            if (!configuration.enforceSsl) {
                ServerConnector standardConnector = new ServerConnector(server);
                standardConnector.setPort(configuration.port);
                server.addConnector(standardConnector);
            }
        }

        config.enableCorsForAllOrigins();
        config.enforceSsl = configuration.enforceSsl;
        config.showJavalinBanner = false;

        if (configuration.debugEnabled) {
            config.requestCacheSize = FilesUtils.displaySizeToBytesCount(System.getProperty("reposilite.requestCacheSize", "8MB"));
            Reposilite.getLogger().debug("requestCacheSize set to " + config.requestCacheSize + " bytes");
            Reposilite.getLogger().info("Debug enabled");
            config.enableDevLogging();
        }

        config.server(() -> server);
    }

    void stop() {
        if (javalin != null) {
            javalin.stop();
        }
    }

    public boolean isAlive() {
        return getJavalin()
                .map(Javalin::server)
                .map(JavalinServer::server)
                .map(Server::isStarted)
                .orElseGet(false);
    }

    public Option<Javalin> getJavalin() {
        return Option.of(javalin);
    }

}