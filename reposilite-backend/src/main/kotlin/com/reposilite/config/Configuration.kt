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
import com.reposilite.shared.Validator
import net.dzikoysk.cdn.entity.Contextual
import net.dzikoysk.cdn.entity.Description
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.Serializable

class Configuration : Serializable {

    /* General */

    @Description(
        "# ~~~~~~~~~~~~~~~~~~~~~~ #",
        "#       Reposilite       #",
        "# ~~~~~~~~~~~~~~~~~~~~~~ #",
        "",
        "# Hostname",
        "# The hostname can be used to limit which connections are accepted.",
        "# Use 0.0.0.0 to accept connections from anywhere." ,
        "# 127.0.0.1 will only allow connections from localhost. "
    )
    @JvmField
    var hostname = "0.0.0.0"

    @Description("# Port to bind")
    @JvmField
    var port = 80

    @Description(
        "# Database. Supported storage providers:",
        "# - sqlite reposilite.db",
        "# - sqlite --temporary",
        "# - mysql localhost:3306 database user password"
    )
    @JvmField
    var database = "sqlite reposilite.db"

    @Command(name = "sqlite")
    internal class SQLiteDatabaseSettings : Validator() {

        @Parameters(index = "0", paramLabel = "<file-name>", defaultValue = "")
        var fileName: String = ""

        @Option(names = ["--temporary", "--temp", "-t"])
        var temporary = false

    }

    @Command(name = "mysql")
    internal class MySqlDatabaseSettings : Validator() {

        @Parameters(index = "0", paramLabel = "<host>")
        lateinit var host: String

        @Parameters(index = "1", paramLabel = "<database>")
        lateinit var database: String

        @Parameters(index = "2", paramLabel = "<user>")
        lateinit var user: String

        @Parameters(index = "3", paramLabel = "<password>")
        lateinit var password: String

    }

    // @Description("# Run Reposilite using Jakarta Servlet server (not supported yet)")
    // public boolean servlet = false;

    @Description("", "# Repository id used in Maven repository configuration")
    @JvmField
    var id = "reposilite-repository"

    @Description("# Repository title")
    @JvmField
    var title = "Reposilite Repository"

    @Description("# Repository description")
    @JvmField
    var description = "Public Maven repository hosted through the Reposilite"

    @Description("# Link to organization's website")
    @JvmField
    var organizationWebsite = "https://reposilite.com"

    @Description("# Link to organization's logo")
    @JvmField
    var organizationLogo = "https://avatars.githubusercontent.com/u/88636591"

    @Description(
        "# The Internet Content Provider License (also known as Bei'An)",
        "# Web services in China require ICP license, a permit issued by the Chinese government to permit China-based websites to operate in China.",
        "# In order to fulfill the conditions, you should apply for ICP license from your service provider and fill in this parameter.",
    )
    @JvmField
    var icpLicense = ""

    @Description("# Enable default frontend with dashboard")
    @JvmField
    var frontend = true

    @Description("# Custom base path")
    @JvmField
    var basePath = "/"

    @Description("# Keep processed frontend files in memory to improve response time")
    @JvmField
    var cacheContent = true

    @Description("# Enable Swagger (/swagger-docs) and Swagger UI (/swagger)")
    @JvmField
    var swagger = false

    /* Repository properties */

    @Description(
        "",
        "# List of supported Maven repositories"
    )
    @JvmField
    var repositories = mutableMapOf(
        "releases" to RepositoryConfiguration(),
        "snapshots" to RepositoryConfiguration(),
        "private" to RepositoryConfiguration().also { it.visibility = PRIVATE }
    )

    @Contextual
    class RepositoryConfiguration : Serializable {

        @Description("# Supported visibilities: public, hidden, private")
        @JvmField
        var visibility = RepositoryVisibility.PUBLIC

        @Description("# Does this repository accept redeployment of the same artifact version")
        @JvmField
        var redeployment = false

        @Description(
            "",
            "# Used storage type. Supported storage providers:",
            "# > File system (local) provider. Supported flags:",
            "# --quota 10GB = control the maximum amount of data stored in this repository. (Supported formats: 90%, 500MB, 10GB)",
            "# Example usage:",
            "# storageProvider: fs --quota 50GB",
            "# > S3 provider. Supported flags:",
            "# --endpoint = custom endpoint with which the S3 provider should communicate (optional)",
            "# Example usage:",
            "# storageProvider: s3 --endpoint custom.endpoint.com accessKey secretKey region bucket-name"
        )
        @JvmField
        var storageProvider = "fs --quota 100%"

        @Command(name = "fs", description = ["Local file system (disk) storage provider settings"])
        internal class FSStorageProviderSettings : Validator() {

            @Option(names = ["-q", "--quota"], defaultValue = "100%")
            lateinit var quota: String

        }

        @Command(name = "s3", description = ["Amazon S3 storage provider settings"])
        internal class S3StorageProviderSettings : Validator() {

            @Option(names = ["-e", "--endpoint"], defaultValue = "")
            lateinit var endpoint: String

            @Parameters(index = "0", paramLabel = "<access-key>")
            lateinit var accessKey: String

            @Parameters(index = "1", paramLabel = "<secret-key>")
            lateinit var secretKey: String

            @Parameters(index = "2", paramLabel = "<region>")
            lateinit var region: String

            @Parameters(index = "3", paramLabel = "<bucket-name>")
            lateinit var bucketName: String

        }

        @Description(
            "",
            "# List of proxied repositories associated with this repository.",
            "# Reposilite will search for a requested artifact in remote repositories listed below.",
            "# Supported flags:",
            "# --store - Reposilite can store proxied artifacts locally to reduce response time and improve stability",
            "# --connectTimeout=<seconds> - How long Reposilite can wait for establishing the connection with a remote host (default: 3s)",
            "# --readTimeout=<seconds> - How long Reposilite can read data from remote proxy. (default: 15s)",
            "# Example usage:",
            "# proxied [",
            "#   https://repo.panda-lang.org/releases --store --connectTimeout=3 --readTimeout=15 --auth user:token",
            "# ]"
        )
        @JvmField
        var proxied = mutableListOf<String>()

        @Command(description = ["An entry representing one proxied host and its configuration"])
        class ProxiedHostConfiguration : Validator() {

            @Option(names = ["--store"])
            var store = false

            @Option(names = ["--connectTimeout"])
            var connectTimeout = 3

            @Option(names = ["--readTimeout"])
            var readTimeout = 15

            @Option(names = ["--authorization", "--auth"])
            var authorization: String? = null

        }

    }

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

    /* Performance */

//    @Description(
//        "",
//        "# Note: It might be hard to estimate the best amount of threads for your use case,",
//        "# but you can safely increase amount of threads if needed and Reposilite will create only as much as it needs.",
//        "# This option might be more useful to limit available memory resources to minimum (1 thread requires around 200kb to 1MB of memory)",
//        "",
//        "# By default, Reposilite 3.x uses experimental reactive mode to maximize performance of each spawned thread.",
//        "# If you've noticed various unresolved behaviours like freezing and deadlocking, you can switch to the standard blocking mode.",
//        "# Remember: Blocking mode requires more resources (threads) to handle the same throughput. "
//    )
//    @JvmField
//    var reactiveMode = true

    @Description(
        "",
        "# Max amount of threads used by core thread pool (min: 4)",
        "# The web thread pool handles first few steps of incoming http connections, as soon as possible all tasks are redirected to IO thread pool."
    )
    @JvmField
    var webThreadPool = 32

    @Description(
        "# IO thread pool handles all tasks that may benefit from non-blocking IO (min: 2)",
        "# Because most of tasks are redirected to IO thread pool, it might be a good idea to keep it at least equal to web thread pool."
    )
    @JvmField
    var ioThreadPool = 16

    /* Logging */

    @Description("", "# Amount of messages stored in cached logger.")
    @JvmField
    var cachedLogSize = 100

    @Description(
        "# Any kind of proxy services change real ip.",
        "# The origin ip should be available in one of the headers.",
        "# Nginx: X-Forwarded-For",
        "# Cloudflare: CF-Connecting-IP",
        "# Popular: X-Real-IP"
    )
    @JvmField
    var forwardedIp = "X-Forwarded-For"

    @Description("# Debug mode")
    @JvmField
    var debugEnabled = false

}