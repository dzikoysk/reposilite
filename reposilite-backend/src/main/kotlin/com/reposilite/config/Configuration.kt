/*
 * Copyright (c) 2021 dzikoysk
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
package com.reposilite.config

import com.reposilite.maven.api.RepositoryVisibility
import com.reposilite.maven.api.RepositoryVisibility.PRIVATE
import net.dzikoysk.cdn.entity.Contextual
import net.dzikoysk.cdn.entity.Description
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.Serializable

class Configuration : Serializable {

    /* General */

    @Description(
        "# ~~~~~~~~~~~~~~~~~~~~~~ #",
        "#       Reposilite       #",
        "# ~~~~~~~~~~~~~~~~~~~~~~ #",
        "",
        "# Hostname"
    )
    @JvmField
    var hostname = "127.0.0.1"

    @Description("# Port to bind")
    @JvmField
    var port = 80

    @Description("# Custom base path")
    @JvmField
    var basePath = "/"

    @Description(
        "# Any kind of proxy services change real ip.",
        "# The origin ip should be available in one of the headers.",
        "# Nginx: X-Forwarded-For",
        "# Cloudflare: CF-Connecting-IP",
        "# Popular: X-Real-IP"
    )
    @JvmField
    var forwardedIp = "X-Forwarded-For"

    @Description("# Enable Swagger (/swagger-docs) and Swagger UI (/swagger)")
    @JvmField
    var swagger = false

    // @Description("# Run Reposilite using Jakarta Servlet server (not supported yet)")
    // public boolean servlet = false;

    /* SSL */

    @Description(
        "",
        "# Support encrypted connections"
    )
    @JvmField
    var sslEnabled = false

    @Description("# SSL port to bind")
    @JvmField
    var sslPort = 443

    @Description(
        "# Key store file to use.",
        "# You can specify absolute path to the given file or use \${WORKING_DIRECTORY} variable."
    )
    @JvmField
    var keyStorePath = "\${WORKING_DIRECTORY}/keystore.jks"

    @Description("# Key store password to use")
    @JvmField
    var keyStorePassword = ""

    @Description("# Redirect http traffic to https")
    @JvmField
    var enforceSsl = false

    /* Repository properties */

    @Description(
        "",
        "# List of supported Maven repositories.",
        "# First directory on the list is the main (primary) repository.",
        "# Tu mark repository as private, add the \"--private\" flag"
    )
    @JvmField
    var repositories: Map<String, RepositoryConfiguration> = mutableMapOf(
        "releases" to RepositoryConfiguration(),
        "snapshots" to RepositoryConfiguration(),
        "private" to RepositoryConfiguration().also { it.visibility = PRIVATE }
    )

    @Contextual
    class RepositoryConfiguration : Serializable {

        @Description("# Supported visibilities: public, hidden, private")
        @JvmField
        var visibility = RepositoryVisibility.PUBLIC

        @Description(
            "# Used storage type. Supported storage providers:",
            "# - fs",
            "# - s3 bucket-name region"
        )
        @JvmField
        var storageProvider = "fs"

        @Description(
            "# Control the maximum amount of data stored in this repository",
            "# Supported formats: 90%, 500MB, 10GB"
        )
        @JvmField
        var diskQuota = "10GB"

        @Description("# Does this repository accept redeployment of the same artifact version")
        @JvmField
        var redeployment = false

        @Description(
            "# List of proxied repositories associated with this repository.",
            "# Reposilite will search for a requested artifact in remote repositories listed below.",
            "# Supported flags:",
            "# --store - Reposilite can store proxied artifacts locally to reduce response time and improve stability",
            "# --connectTimeout=<seconds> - How long Reposilite can wait for establishing the connection with a remote host (default: 3s)",
            "# --readTimeout=<seconds> - How long Reposilite can read data from remote proxy. (default: 15s)",
            "# Example usage:",
            "# proxied [",
            "#   https://repo.panda-lang.org --store --connectTimeout=3 --readTimeout=15 --auth user:token",
            "# ]"
        )
        @JvmField
        var proxied = mutableListOf<String>()

        @Command(description = ["An entry representing one proxied host and its configuration"])
        class ProxiedHostConfiguration : Runnable {

            @Option(names = ["--store"])
            var store = false

            @Option(names = ["--connectTimeout"])
            var connectTimeout = 3

            @Option(names = ["--readTimeout"])
            var readTimeout = 15

            @Option(names = ["--authorization", "--auth"])
            var authorization: String? = null

            override fun run() { }

        }

    }

    /* Frontend properties */

    @Description(
        "",
        "# Title displayed by frontend"
    )
    @JvmField
    var title = "#onlypanda"

    @Description("# Description displayed by frontend")
    @JvmField
    var description = "Public Maven repository hosted through the Reposilite"

    /* Performance & Debug */

    @Description(
        "",
        "# Note: It might be hard to estimate the best amount of threads for your use case,",
        "# but you can safely increase amount of threads if needed and Reposilite will create only as much as it needs.",
        "# This option might be more useful to limit available memory resources to minimum (1 thread requires ~1MB of memory)",
        "",
        "# Max amount of threads used by core thread pool (min: 4)",
    )
    @JvmField
    var coreThreadPool = 8

    @Description("# Amount of messages stored in cached logger.")
    @JvmField
    var cachedLogSize = 100

    @Description("# Debug mode")
    @JvmField
    var debugEnabled = false

}