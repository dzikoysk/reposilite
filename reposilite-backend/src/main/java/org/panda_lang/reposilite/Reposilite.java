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

import io.vavr.control.Try;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.TokenService;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.config.ConfigurationLoader;
import org.panda_lang.reposilite.console.Console;
import org.panda_lang.reposilite.frontend.Frontend;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.repository.RepositoryService;
import org.panda_lang.reposilite.stats.StatsService;
import org.panda_lang.reposilite.utils.TimeUtils;
import org.panda_lang.utilities.commons.console.Effect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Reposilite {

    private static final Logger LOGGER = LoggerFactory.getLogger("Reposilite");

    private final Console console = new Console(this);
    private final TokenService tokenService = new TokenService();
    private final StatsService statsService = new StatsService();
    private final MetadataService metadataService = new MetadataService();
    private final RepositoryService repositoryService = new RepositoryService();
    private final Authenticator authenticator = new Authenticator(tokenService);
    private final ReposiliteHttpServer reactiveHttpServer = new ReposiliteHttpServer(this);
    private final Frontend frontend = Frontend.createInstance();
    private Configuration configuration;
    private boolean stopped;
    private long uptime;

    public static void main(String[] args) throws Exception {
        Reposilite reposilite = new Reposilite();
        reposilite.launch(args);
    }

    public void launch(String[] args) throws Exception {
        if (console.executeArguments(args)) {
            return;
        }

        Thread shutdownHook = new Thread(() -> Try.run(this::shutdown).orElseRun(Throwable::printStackTrace));
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        getLogger().info("");
        getLogger().info(Effect.GREEN + "Reposilite " + Effect.RESET + ReposiliteConstants.VERSION);
        getLogger().info("");

        Reposilite.getLogger().info("--- Preparing workspace");
        ConfigurationLoader configurationLoader = new ConfigurationLoader();
        this.configuration = configurationLoader.load();

        getLogger().info("--- Loading data");
        tokenService.load();
        statsService.load();

        getLogger().info("");
        repositoryService.load(configuration);
        repositoryService.scan(configuration);
        getLogger().info("");

        getLogger().info("Binding server at *::" + configuration.getPort());
        this.uptime = System.currentTimeMillis();

        reactiveHttpServer.start(configuration, () -> {
            getLogger().info("Done (" + TimeUtils.format(TimeUtils.getUptime(uptime)) + "s)!");
            console.displayHelp();

            getLogger().info("Collecting status metrics...");
            console.displayStatus();
        });

        console.hook();
    }

    public void shutdown() throws Exception {
        if (stopped) {
            return;
        }

        this.stopped = true;
        getLogger().info("Shutting down...");

        statsService.save();
        reactiveHttpServer.stop();
        console.stop();

        getLogger().info("Bye! Uptime: " + TimeUtils.format(TimeUtils.getUptime(uptime) / 60) + "min");
    }

    public long getUptime() {
        return System.currentTimeMillis() - uptime;
    }

    public ReposiliteHttpServer getHttpServer() {
        return reactiveHttpServer;
    }

    public Frontend getFrontend() {
        return frontend;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public MetadataService getMetadataService() {
        return metadataService;
    }

    public StatsService getStatsService() {
        return statsService;
    }

    public TokenService getTokenService() {
        return tokenService;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

}
