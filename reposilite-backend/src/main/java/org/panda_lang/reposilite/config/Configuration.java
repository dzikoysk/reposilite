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

package org.panda_lang.reposilite.config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// @formatter:off
@SuppressWarnings("unused")
public final class Configuration implements Serializable {

    // Bind properties
    private String hostname = "";
    private int port = 80;
    private String basePath = "/";
    private boolean debugEnabled = false;

    // Repository properties
    private List<String> repositories = Arrays.asList("releases", "snapshots");
    private List<String> proxied = Collections.emptyList();

    // Access properties
    private boolean deployEnabled = true;
    private boolean rewritePathsEnabled = true;
    private boolean indexingEnabled = true;
    private List<String> managers = Collections.emptyList();

    // Frontend properties
    private String title = "#onlypanda";
    private String description = "Public Maven repository hosted through the Reposilite";
    private String accentColor = "#2fd4aa";

    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getHostname() { return hostname; }

    public void setPort(int port) { this.port = port; }
    public int getPort() { return port; }

    public void setBasePath(String basePath) { this.basePath = basePath; }
    public String getBasePath() { return basePath; }

    public void setDebugEnabled(boolean debugEnabled) { this.debugEnabled = debugEnabled; }
    public boolean isDebugEnabled() { return debugEnabled; }

    public void setProxied(List<String> proxied) { this.proxied = proxied; }
    public List<String> getProxied() { return proxied; }

    public void setRepositories(List<String> repositories) { this.repositories = repositories; }
    public List<String> getRepositories() { return repositories; }

    public void setDeployEnabled(boolean deployEnabled) { this.deployEnabled = deployEnabled; }
    public boolean isDeployEnabled() { return deployEnabled; }

    public void setRewritePathsEnabled(boolean rewritePathsEnabled) { this.rewritePathsEnabled = rewritePathsEnabled; }
    public boolean isRewritePathsEnabled() { return rewritePathsEnabled; }

    public void setIndexingEnabled(boolean indexingEnabled) { this.indexingEnabled = indexingEnabled; }
    public boolean isIndexingEnabled() { return indexingEnabled; }

    public void setManagers(List<String> managers) { this.managers = managers; }
    public List<String> getManagers() { return managers; }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return description; }

    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }
    public String getAccentColor() { return accentColor; }

}
