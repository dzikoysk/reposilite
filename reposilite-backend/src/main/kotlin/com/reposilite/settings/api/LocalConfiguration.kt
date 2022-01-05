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
package com.reposilite.settings.api

import com.reposilite.shared.extensions.Validator
import net.dzikoysk.cdn.entity.Description
import panda.std.reactive.reference
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.Serializable

class LocalConfiguration : Serializable {

    /* General */

    @Description("# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #")
    @Description("#       Reposilite :: Local       #")
    @Description("# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #")
    @Description("")
    @Description("# Hostname")
    @Description("# The hostname can be used to limit which connections are accepted.")
    @Description("# Use 0.0.0.0 to accept connections from anywhere." )
    @Description("# 127.0.0.1 will only allow connections from localhost.")
    val hostname = reference("0.0.0.0")

    @Description("# Port to bind")
    val port = reference(80)

    /* Database */

    @Description("# Database configuration. Supported storage providers:")
    @Description("# - mysql localhost:3306 database user password")
    @Description("# - sqlite reposilite.db")
    @Description("# - sqlite --temporary")
    @Description("# Experimental providers (not covered with tests):")
    @Description("# - postgresql localhost:5432 database user password")
    @Description("# - h2 reposilite")
    val database = reference("sqlite reposilite.db")

    @Command(name = "embedded")
    internal class EmbeddedSQLDatabaseSettings : Validator() {
        @Parameters(index = "0", paramLabel = "<file-name>", defaultValue = "")
        var fileName: String = ""
        @Option(names = ["--temporary", "--temp", "-t"])
        var temporary = false
    }

    @Command(name = "standard")
    internal class StandardSQLDatabaseSettings : Validator() {
        @Parameters(index = "0", paramLabel = "<host>")
        lateinit var host: String
        @Parameters(index = "1", paramLabel = "<database>")
        lateinit var database: String
        @Parameters(index = "2", paramLabel = "<user>")
        lateinit var user: String
        @Parameters(index = "3", paramLabel = "<password>")
        lateinit var password: String
    }

    /* SSL */

    @Description("")
    @Description("# Support encrypted connections")
    val sslEnabled = reference(false)

    @Description("# SSL port to bind")
    val sslPort = reference(443)

    @Description("# Key store file to use.")
    @Description("# You can specify absolute path to the given file or use \${WORKING_DIRECTORY} variable.")
    val keyStorePath = reference("\${WORKING_DIRECTORY}/keystore.jks")

    @Description("# Key store password to use")
    val keyStorePassword = reference("")

    @Description("# Redirect http traffic to https")
    val enforceSsl = reference(false)

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
//    var reactiveMode = true

    @Description("")
    @Description("# Max amount of threads used by core thread pool (min: 4)")
    @Description("# The web thread pool handles first few steps of incoming http connections, as soon as possible all tasks are redirected to IO thread pool.")
    val webThreadPool = reference(32)

    @Description("# IO thread pool handles all tasks that may benefit from non-blocking IO (min: 2)")
    @Description("# Because most of tasks are redirected to IO thread pool, it might be a good idea to keep it at least equal to web thread pool.")
    val ioThreadPool = reference(16)

    @Description("# Select compression strategy used by this instance.")
    @Description("# Using 'none' reduces usage of CPU & memory, but ends up with higher transfer usage.")
    @Description("# GZIP is better option if you're not limiting resources that much to increase overall request times.")
    @Description("# Available strategies: none, gzip")
    val compressionStrategy = reference("none")

    @Description("", "# Keep processed frontend files in memory to improve response time")
    val cacheContent = reference(true)

    @Description("# Amount of messages stored in cached logger.")
    val cachedLogSize = reference(100)

    @Description("# Debug mode")
    val debugEnabled = reference(false)

}