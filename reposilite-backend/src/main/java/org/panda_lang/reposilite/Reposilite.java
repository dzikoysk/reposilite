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

import org.panda_lang.reposilite.auth.AuthService;
import org.panda_lang.reposilite.auth.AuthenticationConfiguration;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.TokenService;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.config.ConfigurationLoader;
import org.panda_lang.reposilite.console.Console;
import org.panda_lang.reposilite.console.ConsoleConfiguration;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.resource.FrontendProvider;
import org.panda_lang.reposilite.metadata.MetadataConfiguration;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.repository.*;
import org.panda_lang.reposilite.stats.StatsConfiguration;
import org.panda_lang.reposilite.stats.StatsService;
import org.panda_lang.reposilite.storage.FileSystemStorageProvider;
import org.panda_lang.reposilite.storage.StorageProvider;
import org.panda_lang.reposilite.utils.RunUtils;
import org.panda_lang.reposilite.utils.TimeUtils;
import org.panda_lang.utilities.commons.ValidationUtils;
import org.panda_lang.utilities.commons.function.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Reposilite {

    private static final Logger LOGGER = LoggerFactory.getLogger("Reposilite");

    private final boolean servlet;
    private final AtomicBoolean alive;
    private final ExecutorService ioService;
    private final ScheduledExecutorService retryService;
    private final Path configurationFile;
    private final Path workingDirectory;
    private final boolean testEnvEnabled;
    private final StorageProvider storageProvider;
    private final Configuration configuration;
    private final ReposiliteContextFactory contextFactory;
    private final ReposiliteExecutor executor;
    private final FailureService failureService;
    private final Authenticator authenticator;
    private final RepositoryAuthenticator repositoryAuthenticator;
    private final AuthService authService;
    private final TokenService tokenService;
    private final StatsService statsService;
    private final RepositoryService repositoryService;
    private final MetadataService metadataService;
    private final LookupService lookupService;
    private final ProxyService proxyService;
    private final DeployService deployService;
    private final FrontendProvider frontend;
    private final ReposiliteHttpServer reactiveHttpServer;
    private final Console console;
    private final Thread shutdownHook;
    private long uptime;

    Reposilite(Path configurationFile, Path workingDirectory, boolean servlet, boolean testEnv) {
        ValidationUtils.notNull(configurationFile, "Configuration file cannot be null. To use default configuration file, provide empty string");
        ValidationUtils.notNull(workingDirectory, "Working directory cannot be null. To use default working directory, provide empty string");

        this.servlet = servlet;
        this.alive = new AtomicBoolean(false);
        this.ioService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 1, TimeUnit.SECONDS, new SynchronousQueue<>());
        this.retryService = Executors.newSingleThreadScheduledExecutor();
        this.configurationFile = configurationFile;
        this.workingDirectory = workingDirectory;
        this.testEnvEnabled = testEnv;

        this.storageProvider = new FileSystemStorageProvider(Paths.get(""));
        this.configuration = ConfigurationLoader.tryLoad(configurationFile);
        this.contextFactory = new ReposiliteContextFactory(configuration.forwardedIp);
        this.failureService = new FailureService();
        this.executor = new ReposiliteExecutor(testEnvEnabled, failureService);
        this.tokenService = new TokenService(workingDirectory);
        this.statsService = new StatsService(workingDirectory, failureService, ioService, retryService);
        this.repositoryService = new RepositoryService(workingDirectory, configuration.diskQuota, ioService, retryService, failureService);
        this.metadataService = new MetadataService(failureService);

        this.authenticator = new Authenticator(repositoryService, tokenService);
        this.repositoryAuthenticator = new RepositoryAuthenticator(configuration.rewritePathsEnabled, authenticator, repositoryService);
        this.authService = new AuthService(authenticator);
        this.deployService = new DeployService(configuration.deployEnabled, configuration.rewritePathsEnabled, authenticator, repositoryService, metadataService);
        this.lookupService = new LookupService(repositoryAuthenticator, metadataService, repositoryService);
        this.proxyService = new ProxyService(configuration.storeProxied, configuration.rewritePathsEnabled, configuration.proxied, ioService, failureService, repositoryService);
        this.frontend = FrontendProvider.load(configuration);
        this.reactiveHttpServer = new ReposiliteHttpServer(this, servlet);
        this.console = new Console(System.in, failureService);
        this.shutdownHook = new Thread(RunUtils.ofChecked(failureService, this::shutdown));
    }

    public ReposiliteConfiguration[] configurations() {
        return new ReposiliteConfiguration[] {
                new AuthenticationConfiguration(),
                new ConsoleConfiguration(),
                new MetadataConfiguration(),
                new StatsConfiguration()
        };
    }

    public void launch() throws Exception {
        load();
        start();
    }

    public void load() throws Exception {
        getLogger().info("--- Environment");

        if (isTestEnvEnabled()) {
            getLogger().info("Test environment enabled");
        }

        getLogger().info("Platform: " + System.getProperty("java.version") + " (" + System.getProperty("os.name") + ")");
        getLogger().info("Configuration: " + configurationFile.toAbsolutePath());
        getLogger().info("Working directory: " + workingDirectory.toAbsolutePath());
        getLogger().info("");

        this.alive.set(true);
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);

        getLogger().info("--- Loading data");
        tokenService.loadTokens();

        getLogger().info("");
        repositoryService.load(configuration);
        getLogger().info("");

        getLogger().info("--- Loading domain configurations");
        for (ReposiliteConfiguration configuration : configurations()) {
            configuration.configure(this);
        }
    }

    public void start() throws Exception {
        getLogger().info("Binding server at " + configuration.hostname + "::" + configuration.port);

        CountDownLatch latch = new CountDownLatch(1);
        this.uptime = System.currentTimeMillis();

        reactiveHttpServer.start(configuration, () -> {
            getLogger().info("Done (" + TimeUtils.format(TimeUtils.getUptime(uptime)) + "s)!");

            schedule(() -> {
                console.defaultExecute("help");

                getLogger().info("Collecting status metrics...");
                console.defaultExecute("status");

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

        reactiveHttpServer.stop();
        statsService.saveStats();
        ioService.shutdownNow();
        retryService.shutdownNow();
        console.stop();
        executor.stop();
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

    public FrontendProvider getFrontendService() {
        return frontend;
    }

    public ProxyService getProxyService() {
        return proxyService;
    }

    public LookupService getLookupService() {
        return lookupService;
    }

    public DeployService getDeployService() {
        return deployService;
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

    public AuthService getAuthService() {
        return authService;
    }

    public RepositoryAuthenticator getRepositoryAuthenticator() {
        return repositoryAuthenticator;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public FailureService getFailureService() {
        return failureService;
    }

    public ReposiliteContextFactory getContextFactory() {
        return contextFactory;
    }

    public Console getConsole() {
        return console;
    }

    public ReposiliteExecutor getExecutor() {
        return executor;
    }

    public ScheduledExecutorService getRetryService() {
        return retryService;
    }

    public ExecutorService getIoService() {
        return ioService;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

}
