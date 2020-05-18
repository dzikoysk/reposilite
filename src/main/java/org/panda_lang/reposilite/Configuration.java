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

import java.io.Serializable;
import java.util.List;

public final class Configuration implements Serializable {

    private String hostname;
    private int port;
    private List<String> repositories;
    private boolean deployEnabled;
    private boolean rewritePathsEnabled;
    private boolean fullAuthEnabled;

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setRepositories(List<String> repositories) {
        this.repositories = repositories;
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

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
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
