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

import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.TokenService;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.config.ConfigurationLoader;
import org.panda_lang.reposilite.console.Console;
import org.panda_lang.reposilite.frontend.FrontendService;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.repository.RepositoryService;
import org.panda_lang.reposilite.stats.StatsService;
import org.panda_lang.reposilite.utils.FutureUtils;
import org.panda_lang.reposilite.utils.TimeUtils;
import org.panda_lang.utilities.commons.ValidationUtils;
import org.panda_lang.utilities.commons.collection.Pair;
import org.panda_lang.utilities.commons.function.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Reposilite {

    private static final Logger LOGGER = LoggerFactory.getLogger("Reposilite");

    private final AtomicBoolean alive;
    private final Collection<Pair<String, Throwable>> exceptions;
    private final File configurationFile;
    private final File workingDirectory;
    private final boolean testEnvEnabled;
    private final Configuration configuration;
    private final ReposiliteExecutor executor;
    private final Authenticator authenticator;
    private final TokenService tokenService;
    private final StatsService statsService;
    private final RepositoryService repositoryService;
    private final MetadataService metadataService;
    private final FrontendService frontend;
    private final ReposiliteHttpServer reactiveHttpServer;
    private final Console console;
    private final Thread shutdownHook;
    private long uptime;

    Reposilite(String configurationFile, String workingDirectory, boolean testEnv) {
        ValidationUtils.notNull(configurationFile, "Configuration file cannot be null. To use default configuration file, provide empty string");
        ValidationUtils.notNull(workingDirectory, "Working directory cannot be null. To use default working directory, provide empty string");

        this.alive = new AtomicBoolean(false);
        this.exceptions = new ArrayList<>();
        this.configurationFile = new File(configurationFile);
        this.workingDirectory = new File(workingDirectory);
        this.testEnvEnabled = testEnv;

        this.configuration = ConfigurationLoader.tryLoad(configurationFile, workingDirectory);
        this.executor = new ReposiliteExecutor(this);
        this.tokenService = new TokenService(workingDirectory);
        this.statsService = new StatsService(workingDirectory);
        this.repositoryService = new RepositoryService(workingDirectory);
        this.metadataService = new MetadataService(this);

        this.authenticator = new Authenticator(configuration, repositoryService, tokenService);
        this.frontend = FrontendService.load(configuration);
        this.reactiveHttpServer= new ReposiliteHttpServer(this);
        this.console = new Console(this, System.in);
        this.shutdownHook = new Thread(FutureUtils.ofChecked(this, this::shutdown));
    }

    public void launch() throws Exception {
        getLogger().info("--- Environment");

        if (isTestEnvEnabled()) {
            getLogger().info("Test environment enabled");
        }

        getLogger().info("Platform: " + System.getProperty("java.version") + " (" + System.getProperty("os.name") + ")");
        getLogger().info("Configuration: " + configurationFile.getAbsolutePath());
        getLogger().info("Working directory: " + workingDirectory.getAbsolutePath());
        getLogger().info("");

        this.alive.set(true);
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);

        getLogger().info("--- Loading data");
        statsService.load();
        tokenService.load();

        getLogger().info("");
        repositoryService.load(configuration);
        getLogger().info("");

        getLogger().info("Binding server at " + configuration.hostname + "::" + configuration.port);
        this.uptime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(1);

        reactiveHttpServer.start(configuration, () -> {
            getLogger().info("Done (" + TimeUtils.format(TimeUtils.getUptime(uptime)) + "s)!");

            schedule(() -> {
                console.execute("help");

                getLogger().info("Collecting status metrics...");
                console.execute("status");

                // disable console daemon in tests due to issues with coverage and interrupt method call
                // https://github.com/jacoco/jacoco/issues/1066
                if (!isTestEnvEnabled()) {
                    console.hook();
                }
            });

            latch.countDown();
        });

        latch.await();
        executor.await(() -> getLogger().info("Bye! Uptime: " + TimeUtils.format(TimeUtils.getUptime(uptime) / 60) + "min"));
    }

    public synchronized void forceShutdown() throws Exception {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        shutdown();
    }

    public synchronized void shutdown() throws Exception {
        if (!alive.get()) {
            return;
        }

        this.alive.set(false);
        getLogger().info("Shutting down " + configuration.hostname  + "::" + configuration.port + " ...");

        statsService.save();
        reactiveHttpServer.stop();
        console.stop();
        executor.stop();
    }

    public void throwException(String id, Throwable throwable) {
        getLogger().error(id, throwable);
        exceptions.add(new Pair<>(id, throwable));
    }

    public void schedule(ThrowingRunnable<?> runnable) {
        executor.schedule(runnable);
    }

    public boolean isTestEnvEnabled() {
        return testEnvEnabled;
    }

    public long getUptime() {
        return System.currentTimeMillis() - uptime;
    }

    public ReposiliteHttpServer getHttpServer() {
        return reactiveHttpServer;
    }

    public FrontendService getFrontend() {
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

    public Console getConsole() {
        return console;
    }

    public Collection<? extends Pair<String, Throwable>> getExceptions() {
        return exceptions;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

}
