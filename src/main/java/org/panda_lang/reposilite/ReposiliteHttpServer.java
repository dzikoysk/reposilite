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
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.panda_lang.reposilite.api.IndexApiController;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.frontend.FrontendController;
import org.panda_lang.reposilite.repository.DeployController;
import org.panda_lang.reposilite.repository.LookupController;
import org.panda_lang.reposilite.repository.LookupService;
import org.panda_lang.utilities.commons.collection.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public final class ReposiliteHttpServer {

    private final Reposilite reposilite;
    private final Collection<Pair<String, Exception>> exceptions;
    private Javalin javalin;

    ReposiliteHttpServer(Reposilite reposilite) {
        this.reposilite = reposilite;
        this.exceptions = new ArrayList<>();
    }

    void start(Configuration configuration, Runnable onStart) {
        LookupService lookupService = new LookupService(reposilite);
        LookupController lookupController = new LookupController(reposilite.getFrontend(), lookupService);

        this.javalin = Javalin.create(this::config)
                .get("/", new FrontendController(reposilite))
                .get("/api/*", new IndexApiController(reposilite))
                .get("/*", lookupController)
                .head("/*", lookupController)
                .put("/*", new DeployController(reposilite))
                .exception(Exception.class, (exception, ctx) -> {
                    exception.printStackTrace();
                    exceptions.add(new Pair<>(ctx.req.getRequestURI(), exception));
                })
                .before(ctx -> reposilite.getStatsService().record(ctx.req.getRequestURI()))
                .start(configuration.getHostname(), configuration.getPort());

        onStart.run();
    }

    private void config(JavalinConfig config) {
        config.server(() -> new Server(new QueuedThreadPool(4 * Runtime.getRuntime().availableProcessors())));
        config.showJavalinBanner = false;
    }

    void stop() {
        if (javalin != null) {
            javalin.stop();
        }
    }

    public boolean isAlive() {
        return Objects.requireNonNull(javalin.server()).server().isRunning();
    }

    public Collection<Pair<String, Exception>> getExceptions() {
        return exceptions;
    }

}
