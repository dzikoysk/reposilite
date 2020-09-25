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
import org.panda_lang.reposilite.auth.AuthController;
import org.panda_lang.reposilite.auth.PostAuthHandler;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.console.CliController;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.frontend.FrontendController;
import org.panda_lang.reposilite.repository.DeployController;
import org.panda_lang.reposilite.repository.LookupApiController;
import org.panda_lang.reposilite.repository.LookupController;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.function.Option;

public final class ReposiliteHttpServer {

    private final Reposilite reposilite;
    private Javalin javalin;

    ReposiliteHttpServer(Reposilite reposilite) {
        this.reposilite = reposilite;
    }

    void start(Configuration configuration, Runnable onStart) {
        FailureService failureService = reposilite.getFailureService();
        DeployController deployController = new DeployController(reposilite.getContextFactory(), reposilite.getDeployService());

        LookupController lookupController = new LookupController(
                configuration,
                reposilite.getContextFactory(),
                reposilite.getIoService(),
                reposilite.getFrontendService(),
                reposilite.getLookupService(),
                reposilite.getRepositoryService(),
                failureService);

        LookupApiController lookupApiController = new LookupApiController(
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

        this.javalin = Javalin.create(config -> config(configuration, config))
                .before(ctx -> reposilite.getStatsService().record(ctx.req.getRequestURI()))
                .get("/js/app.js", new FrontendController(reposilite))
                .get("/api/auth", new AuthController(reposilite.getAuthService()))
                .ws("/api/cli", cliController)
                .get("/api", lookupApiController)
                .get("/api/*", lookupApiController)
                .get("/*", lookupController)
                .head("/*", lookupController)
                .put("/*", deployController)
                .post("/*", deployController)
                .after("/*", new PostAuthHandler())
                .exception(Exception.class, (exception, ctx) -> failureService.throwException(ctx.req.getRequestURI(), exception))
                .start(configuration.hostname, configuration.port);

        onStart.run();
    }

    private void config(Configuration configuration, JavalinConfig config) {
        config.server(Server::new);
        config.showJavalinBanner = false;
        config.enableCorsForAllOrigins();

        if (configuration.debugEnabled) {
            config.requestCacheSize = FilesUtils.displaySizeToBytesCount(System.getProperty("reposilite.requestCacheSize", "8MB"));
            Reposilite.getLogger().debug("requestCacheSize set to " + config.requestCacheSize + " bytes");
            Reposilite.getLogger().info("Debug enabled");
            config.enableDevLogging();
        }
    }

    void stop() {
        if (javalin != null) {
            javalin.stop();
        }
    }

    public boolean isAlive() {
        return Option.of(javalin)
                .map(Javalin::server)
                .map(JavalinServer::server)
                .map(Server::isStarted)
                .orElseGet(false);
    }

}