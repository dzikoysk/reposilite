package org.panda_lang.reposilite;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
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
        javalin.stop();
    }

    public boolean isAlive() {
        return Objects.requireNonNull(javalin.server()).server().isRunning();
    }

    public Collection<Pair<String, Exception>> getExceptions() {
        return exceptions;
    }

}
