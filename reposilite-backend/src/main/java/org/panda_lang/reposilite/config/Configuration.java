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

import net.dzikoysk.cdn.entity.Description;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Configuration implements Serializable {

    @Description("# ~~~~~~~~~~~~~~~~~~~~~~ #")
    @Description("#       Reposilite       #")
    @Description("# ~~~~~~~~~~~~~~~~~~~~~~ #")

    // Bind properties
    @Description("")
    @Description("# Hostname")
    public String hostname = "0.0.0.0";
    @Description("# Port to bind")
    public Integer port = 80;
    @Description("# Custom base path")
    public String basePath = "/";
    @Description("# Any kind of proxy services change real ip.")
    @Description("# The origin ip should be available in one of the headers.")
    @Description("# Nginx: X-Forwarded-For")
    @Description("# Cloudflare: CF-Connecting-IP")
    @Description("# Popular: X-Real-IP")
    public String forwardedIp = "X-Forwarded-For";
    @Description("# Debug")
    public Boolean debugEnabled = false;

    // Repository properties
    @Description("")
    @Description("# Control the maximum amount of data assigned to Reposilite instance")
    @Description("# Supported formats: 90%, 500MB, 10GB")
    public String diskQuota = "10GB";
    @Description("# List of supported Maven repositories.")
    @Description("# First directory on the list is the main (primary) repository.")
    @Description("# Tu mark repository as private, prefix its name with a dot, e.g. \".private\"")
    public List<String> repositories = Arrays.asList("releases", "snapshots");
    @Description("# Allow to omit name of the main repository in request")
    @Description("# e.g. /org/panda-lang/reposilite will be redirected to /releases/org/panda-lang/reposilite")
    public Boolean rewritePathsEnabled = true;

    // Proxy
    @Description("")
    @Description("# List of proxied repositories.")
    @Description("# Reposilite will search for an artifact in remote repositories listed below,")
    @Description("# if the requested artifact was not found.")
    public List<String> proxied = Collections.emptyList();
    @Description("# Reposilite can store proxied artifacts locally to reduce response time and improve stability")
    public Boolean storeProxied = true;

    // Access properties
    @Description("")
    @Description("# Accept deployment connections")
    public Boolean deployEnabled = true;
    @Description("# List of management tokens used by dashboard to access extra options.")
    @Description("# (By default, people are allowed to use standard dashboard options related to the associated path)")
    public List<String> managers = Collections.emptyList();

    // Frontend properties
    @Description("")
    @Description("# Title displayed by frontend")
    public String title = "#onlypanda";
    @Description("# Description displayed by frontend")
    public String description = "Public Maven repository hosted through the Reposilite";
    @Description("# Accent color used by frontend")
    public String accentColor = "#2fd4aa";

}
