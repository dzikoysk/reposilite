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

package org.panda_lang.nanomaven;

import org.panda_lang.nanomaven.auth.Authenticator;
import org.panda_lang.nanomaven.auth.TokenService;
import org.panda_lang.nanomaven.console.NanoConsole;
import org.panda_lang.nanomaven.frontend.Frontend;
import org.panda_lang.nanomaven.frontend.FrontendLoader;
import org.panda_lang.nanomaven.metadata.MetadataService;
import org.panda_lang.nanomaven.repository.RepositoryService;
import org.panda_lang.nanomaven.utils.TimeUtils;
import org.panda_lang.nanomaven.utils.YamlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class NanoMaven {

    private static final Logger LOGGER = LoggerFactory.getLogger("NanoMaven");

    private NanoConsole console;
    private Frontend frontend;
    private Authenticator authenticator;
    private TokenService tokenService;
    private MetadataService metadataService;
    private RepositoryService repositoryService;
    private NanoConfiguration configuration;
    private NanoHttpServer httpServer;
    private boolean stopped;
    private long uptime;

    public static void main(String[] args) throws Exception {
        NanoMaven nanoMaven = new NanoMaven();
        nanoMaven.launch();
    }

    public void launch() throws Exception {
        getLogger().info("");
        getLogger().info(NanoConstants.GREETING_MESSAGE);
        getLogger().info("");

        NanoMaven.getLogger().info("--- Preparing workspace");
        NanoWorkspace workspace = new NanoWorkspace();
        workspace.prepare();

        File configurationFile = new File(NanoConstants.CONFIGURATION_FILE_NAME);
        this.configuration = YamlUtils.load(configurationFile, NanoConfiguration.class);

        this.console = new NanoConsole(this);
        console.hook();

        Thread shutdownHook = new Thread(this::shutdown);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        FrontendLoader frontendLoader = new FrontendLoader();
        this.frontend = frontendLoader.loadFrontend(NanoConstants.FRONTEND_FILE_NAME);

        getLogger().info("--- Loading data");
        this.tokenService = new TokenService();
        tokenService.load();
        getLogger().info("");

        this.authenticator = new Authenticator(tokenService);
        this.metadataService = new MetadataService();

        this.repositoryService = new RepositoryService();
        repositoryService.scan(configuration);
        getLogger().info("");

        getLogger().info("Binding server at *::" + configuration.getPort());
        this.httpServer = new NanoHttpServer(this);
        this.uptime = System.currentTimeMillis();

        try {
            httpServer.start();
            getLogger().info("Done (" + TimeUtils.getUptime(uptime) + "s)!");
            console.displayHelp();
        } catch (Exception exception) {
            exception.printStackTrace();
            shutdown();
        }
    }

    public void shutdown() {
        if (stopped) {
            return;
        }
        this.stopped = true;

        getLogger().info("Shutting down...");
        httpServer.stop();

        console.stop();
        getLogger().info("Bye! Uptime: " + (TimeUtils.getUptime(uptime) / 60) + "min");
    }

    public long getUptime() {
        return System.currentTimeMillis() - uptime;
    }

    public NanoHttpServer getHttpServer() {
        return httpServer;
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

    public TokenService getTokenService() {
        return tokenService;
    }

    public NanoConfiguration getConfiguration() {
        return configuration;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

}
