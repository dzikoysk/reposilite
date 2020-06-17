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
import org.eclipse.jetty.server.Server;
import org.panda_lang.reposilite.auth.AuthApiController;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.config.ConfigurationApiController;
import org.panda_lang.reposilite.frontend.FrontendController;
import org.panda_lang.reposilite.repository.DeployController;
import org.panda_lang.reposilite.repository.IndexApiController;
import org.panda_lang.reposilite.repository.LookupController;
import org.panda_lang.reposilite.repository.LookupService;

import java.util.Objects;

public final class ReposiliteHttpServer {

    private final Reposilite reposilite;
    private Javalin javalin;

    ReposiliteHttpServer(Reposilite reposilite) {
        this.reposilite = reposilite;
    }

    void start(Configuration configuration, Runnable onStart) {
        LookupService lookupService = new LookupService(reposilite);
        LookupController lookupController = new LookupController(reposilite.getFrontend(), lookupService);
        DeployController deployController = new DeployController(reposilite);

        this.javalin = Javalin.create(config -> config(configuration, config))
                .get("/api/auth", new AuthApiController(configuration, reposilite.getAuthenticator()))
                .get("/api/configuration", new ConfigurationApiController(reposilite.getConfiguration()))
                .get("/api/*", new IndexApiController(reposilite))
                .get("/js/app.js", new FrontendController(reposilite))
                .get("/*", lookupController)
                .head("/*", lookupController)
                .put("/*", deployController)
                .post("/*", deployController)
                .before(ctx -> reposilite.getStatsService().record(ctx.req.getRequestURI()))
                .exception(Exception.class, (exception, ctx) -> reposilite.throwException(ctx.req.getRequestURI(), exception))
                .start(configuration.getHostname(), configuration.getPort());

        onStart.run();
    }

    private void config(Configuration configuration, JavalinConfig config) {
        config.server(Server::new);
        config.showJavalinBanner = false;

        if (configuration.isDebugEnabled()) {
            Reposilite.getLogger().info("Debug enabled");
            config.enableCorsForAllOrigins();
            config.enableDevLogging();
        }
    }

    void stop() {
        if (javalin != null) {
            javalin.stop();
        }
    }

    public boolean isAlive() {
        return Objects.requireNonNull(javalin.server()).server().isRunning();
    }

}