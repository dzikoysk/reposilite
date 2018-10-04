/*
 * Copyright (c) 2017 Dzikoysk
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

import org.panda_lang.nanomaven.console.NanoConsole;
import org.panda_lang.nanomaven.console.commands.HelpCommand;
import org.panda_lang.nanomaven.maven.NanoMavenCli;
import org.panda_lang.nanomaven.server.NanoHttpServer;
import org.panda_lang.nanomaven.server.auth.NanoProjectsManager;
import org.panda_lang.nanomaven.server.auth.NanoUsersManager;
import org.panda_lang.nanomaven.util.DirectoryUtils;
import org.panda_lang.nanomaven.util.TimeUtils;
import org.panda_lang.nanomaven.workspace.NanoWorkspace;
import org.panda_lang.nanomaven.workspace.configuration.NanoMavenConfiguration;
import org.panda_lang.nanomaven.workspace.repository.NanoRepositoryManager;
import org.slf4j.Logger;

public class NanoMaven {

    private NanoConsole console;
    private NanoWorkspace workspace;
    private NanoUsersManager usersManager;
    private NanoMavenConfiguration configuration;
    private NanoRepositoryManager repositoryManager;
    private NanoProjectsManager projectsManager;
    private NanoHttpServer httpServer;
    private NanoMavenCli mavenCli;
    private boolean stopped;
    private long uptime;

    private void initialize() {
        this.configuration = new NanoMavenConfiguration();
        configuration.load();

        this.console = new NanoConsole(this);
        console.hook();

        this.workspace = new NanoWorkspace();
        workspace.prepare();

        this.usersManager = new NanoUsersManager();
        usersManager.load(configuration);

        this.repositoryManager = new NanoRepositoryManager();
        repositoryManager.scan();

        this.projectsManager = new NanoProjectsManager(usersManager);
        projectsManager.load();

        this.httpServer = new NanoHttpServer(this);
        this.mavenCli = new NanoMavenCli(this);
    }

    public void launch() {
        getLogger().info(NanoMavenConstants.GREETING_MESSAGE);
        initialize();

        getLogger().info("Binding NanoMaven at *::" + configuration.getPort());
        this.uptime = System.currentTimeMillis();

        try {
            httpServer.start();

            Thread shutdownHook = new Thread(this::shutdown);
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            getLogger().info("Done (" + TimeUtils.getUptime(uptime) + "s)!");

            HelpCommand listCommands = new HelpCommand();
            listCommands.call(this);
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

        getLogger().info("Saving users...");
        usersManager.save();

        getLogger().info("Saving projects...");
        projectsManager.save();

        getLogger().info("Bye! Uptime: " + TimeUtils.getUptime(uptime) + "s");
    }

    public boolean isStopped() {
        return stopped;
    }

    public long getUptime() {
        return uptime;
    }

    public NanoMavenCli getMavenCli() {
        return mavenCli;
    }

    public NanoProjectsManager getProjectsManager() {
        return projectsManager;
    }

    public NanoRepositoryManager getRepositoryManager() {
        return repositoryManager;
    }

    public NanoMavenConfiguration getConfiguration() {
        return configuration;
    }

    public NanoUsersManager getUsersManager() {
        return usersManager;
    }

    public static String getDataFolder() {
        return DirectoryUtils.getDataFolder(NanoMaven.class);
    }

    public static Logger getLogger() {
        return NanoLogger.LOGGER;
    }

}
