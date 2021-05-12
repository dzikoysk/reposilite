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

import net.dzikoysk.cdn.entity.Contextual;
import net.dzikoysk.cdn.entity.Description;
import org.panda_lang.reposilite.repository.RepositoryVisibility;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Configuration implements Serializable {

    @Description("# ~~~~~~~~~~~~~~~~~~~~~~ #")
    @Description("#       Reposilite       #")
    @Description("# ~~~~~~~~~~~~~~~~~~~~~~ #")

    /* General properties */

    @Description("")
    @Description("# Hostname")
    public String hostname = "0.0.0.0";
    @Description("# Port to bind")
    public int port = 80;
    @Description("# Custom base path")
    public String basePath = "/";
    @Description("# Any kind of proxy services change real ip.")
    @Description("# The origin ip should be available in one of the headers.")
    @Description("# Nginx: X-Forwarded-For")
    @Description("# Cloudflare: CF-Connecting-IP")
    @Description("# Popular: X-Real-IP")
    public String forwardedIp = "X-Forwarded-For";
    @Description("# Enable Swagger (/swagger-docs) and Swagger UI (/swagger)")
    public boolean swagger = false;
    @Description("# Debug")
    public boolean debugEnabled = false;

    /* SSL */

    @Description("")
    @Description("# Support encrypted connections")
    public boolean sslEnabled = false;
    @Description("# SSL port to bind")
    public int sslPort = 443;
    @Description("# Key store file to use.")
    @Description("# You can specify absolute path to the given file or use ${WORKING_DIRECTORY} variable.")
    public String keyStorePath = "${WORKING_DIRECTORY}/keystore.jks";
    @Description("# Key store password to use")
    public String keyStorePassword = "";
    @Description("# Redirect http traffic to https")
    public boolean enforceSsl = false;

    /* Repository properties */

    @Description("")
    @Description("# Allow to omit name of the main repository in request")
    @Description("# e.g. /org/panda-lang/reposilite will be redirected to /releases/org/panda-lang/reposilite")
    public boolean rewritePathsEnabled = true;
    // TODO: Remove
    @Description("# Control the maximum amount of data assigned to Reposilite instance")
    @Description("# Supported formats: 90%, 500MB, 10GB")
    public String diskQuota = "10GB";

    @Description("# List of supported Maven repositories.")
    @Description("# First directory on the list is the main (primary) repository.")
    @Description("# Tu mark repository as private, add the \"--private\" flag")
    public Map<String, RepositoryConfiguration> repositories = new LinkedHashMap<String, RepositoryConfiguration>() {{
        put("releases", new RepositoryConfiguration());
        put("snapshots", new RepositoryConfiguration());

        RepositoryConfiguration privateConfiguration = new RepositoryConfiguration();
        privateConfiguration.visibility = RepositoryVisibility.PRIVATE.name().toLowerCase();
        put("private", privateConfiguration);
    }};

    @Contextual
    public static class RepositoryConfiguration implements Serializable {

        @Description("# Supported visibilities: public, hidden, private")
        public String visibility = "public";
        @Description("# Used storage type. Supported storage providers:")
        @Description("# - fs")
        @Description("# - s3 bucket-name region")
        public String storageProvider = "fs";
        @Description("# Control the maximum amount of data stored in this repository")
        @Description("# Supported formats: 90%, 500MB, 10GB")
        public String diskQuota = "10GB";
        @Description("# Accept deployment connections")
        public boolean deployEnabled = true;
        @Description("# Does this repository accept redeployment of the same artifact version")
        public boolean redeploy = false;

    }

    /* Proxy */

    @Description("")
    @Description("# List of proxied repositories.")
    @Description("# Reposilite will search for an artifact in remote repositories listed below,")
    @Description("# if the requested artifact was not found.")
    public List<String> proxied = Collections.emptyList();
    @Description("# Reposilite can store proxied artifacts locally to reduce response time and improve stability")
    public boolean storeProxied = true;
    @Description("# Proxying is disabled by default in private repositories because of the security policy.")
    @Description("# Enabling this feature may expose private data like i.e. artifact name used in your company.")
    public boolean proxyPrivate = false;
    @Description("# How long Reposilite can wait for establishing the connection with a remote host. (In seconds)")
    public int proxyConnectTimeout = 3;
    @Description("# How long Reposilite can read data from remote proxy. (In seconds)")
    @Description("# Increasing this value may be required in case of proxying slow remote repositories.")
    public int proxyReadTimeout = 15;

    /* Frontend properties */

    @Description("")
    @Description("# Title displayed by frontend")
    public String title = "#onlypanda";
    @Description("# Description displayed by frontend")
    public String description = "Public Maven repository hosted through the Reposilite";
    @Description("# Accent color used by frontend")
    public String accentColor = "#2fd4aa";

}
