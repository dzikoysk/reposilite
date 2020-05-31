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
import java.util.ArrayList;
import java.util.List;

public final class Configuration implements Serializable {

    // Bind properties
    private String hostname = "";
    private int port = 80;

    // Repository properties
    private final List<String> repositories = new ArrayList<>(3);
    private final List<String> proxied = new ArrayList<>(3);

    // Access properties
    private boolean deployEnabled = true;
    private boolean rewritePathsEnabled = true;
    private boolean fullAuthEnabled = false;

    // Frontend properties
    private String title = "#onlypanda";
    private String description = "Public Maven repository hosted through the Reposilite";
    private String accentColor = "#009890";

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setRepositories(List<String> repositories) {
        this.repositories.addAll(repositories);
    }

    public void setProxied(List<String> proxied) {
        this.proxied.addAll(proxied);
    }

    public void setDeployEnabled(boolean deployEnabled) {
        this.deployEnabled = deployEnabled;
    }

    public void setRewritePathsEnabled(boolean rewritePathsEnabled) {
        this.rewritePathsEnabled = rewritePathsEnabled;
    }

    public void setFullAuthEnabled(boolean fullAuthEnabled) {
        this.fullAuthEnabled = fullAuthEnabled;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public List<String> getProxied() {
        return proxied;
    }

    public List<String> getRepositories() {
        return repositories;
    }

    public boolean isDeployEnabled() {
        return deployEnabled;
    }

    public boolean isRewritePathsEnabled() {
        return rewritePathsEnabled;
    }

    public boolean isFullAuthEnabled() {
        return fullAuthEnabled;
    }

}
