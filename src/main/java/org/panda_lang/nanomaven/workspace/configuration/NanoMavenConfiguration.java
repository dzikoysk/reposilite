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

package org.panda_lang.nanomaven.workspace.configuration;

import org.panda_lang.panda.utilities.configuration.PandaConfiguration;

import java.io.File;

public class NanoMavenConfiguration {

    private int port;
    private String hostname;
    private boolean repositoryPathEnabled;
    private boolean indexingEnabled;
    private boolean nestedMaven;
    private String externalMaven;

    public void load() {
        File configurationFile = new File("nanomaven.pc");
        PandaConfiguration configuration = new PandaConfiguration(configurationFile);

        this.port = configuration.getInt("port");
        this.hostname = configuration.getString("hostname");

        this.repositoryPathEnabled = configuration.getBoolean("repository-path-enabled");
        this.indexingEnabled = configuration.getBoolean("indexing-enabled");

        this.nestedMaven = configuration.getBoolean("nested-maven");
        this.externalMaven = configuration.getString("external-maven");
    }

    public String getExternalMaven() {
        return externalMaven;
    }

    public boolean isNestedMaven() {
        return nestedMaven;
    }

    public boolean isIndexingEnabled() {
        return indexingEnabled;
    }

    public boolean isRepositoryPathEnabled() {
        return repositoryPathEnabled;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

}
