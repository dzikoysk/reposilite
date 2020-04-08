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

import org.panda_lang.nanomaven.auth.TokenService;
import org.panda_lang.nanomaven.console.HelpCommand;
import org.panda_lang.nanomaven.console.NanoConsole;
import org.panda_lang.nanomaven.frontend.Frontend;
import org.panda_lang.nanomaven.frontend.FrontendLoader;
import org.panda_lang.nanomaven.repository.RepositoryService;
import org.panda_lang.nanomaven.utils.TimeUtils;
import org.panda_lang.nanomaven.utils.YamlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class NanoMaven {

    private static final Logger LOGGER = LoggerFactory.getLogger("NanoMaven");

    private NanoConsole console;
    private Frontend frontend;
    private TokenService tokenService;
    private NanoConfiguration configuration;
    private RepositoryService repositoryService;
    private NanoHttpServer httpServer;
    private boolean stopped;
    private long uptime;

    public static void main(String[] args) throws Exception {
        NanoMaven nanoMaven = new NanoMaven();
        nanoMaven.launch();
    }

    public void launch() throws Exception {
        getLogger().info(NanoConstants.GREETING_MESSAGE);

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

        this.tokenService = new TokenService();
        tokenService.load();

        this.repositoryService = new RepositoryService();
        repositoryService.scan(configuration);

        getLogger().info("Binding at *::" + configuration.getPort());
        this.httpServer = new NanoHttpServer(this);
        this.uptime = System.currentTimeMillis();

        try {
            httpServer.start();
            getLogger().info("Done (" + TimeUtils.getUptime(uptime) + "s)!");

            HelpCommand listCommands = new HelpCommand();
            listCommands.call(this);
        } catch (Exception exception) {
            exception.printStackTrace();
            shutdown();
        }
    }

    public void save() {
        getLogger().info("Saving tokens...");

        try {
            tokenService.save();
        } catch (IOException e) {
            getLogger().info("Failed to save users due to:");
            e.printStackTrace();
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
        getLogger().info("Bye! Uptime: " + TimeUtils.getUptime(uptime) + "s");
    }

    public Frontend getFrontend() {
        return frontend;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public NanoConfiguration getConfiguration() {
        return configuration;
    }

    public TokenService getTokenService() {
        return tokenService;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

}
