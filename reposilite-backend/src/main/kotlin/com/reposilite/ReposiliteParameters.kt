/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite

import com.reposilite.settings.local.LocalConfiguration
import com.reposilite.settings.local.LocalConfigurationMode
import com.reposilite.settings.local.infrastructure.LOCAL_CONFIGURATION_FILE
import com.reposilite.settings.shared.infrastructure.SHARED_CONFIGURATION_FILE
import com.reposilite.token.AccessTokenType.TEMPORARY
import com.reposilite.token.api.CreateAccessTokenRequest
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.nio.file.Path
import java.nio.file.Paths

@Command(name = "reposilite", version = ["Reposilite $VERSION"])
class ReposiliteParameters : Runnable {

    @Option(names = ["--help"], usageHelp = true, description = ["Display help message"])
    var usageHelpRequested = false

    @Option(names = ["--version", "-v"], versionHelp = true, description = ["Display current version of reposilite"])
    var versionInfoRequested = false

    @Option(names = ["--working-directory", "-wd"], description = ["Set custom working directory of application instance"])
    var workingDirectoryName = ""
    lateinit var workingDirectory: Path

    @Option(names = ["--generate-configuration", "-gc"], description = ["" +
        "Generate default template of the configuration file. Supported templates:",
        "configuration.cdn - Local configuration file",
        "configuration.shared.json - Shared configuration file"
    ])
    var configurationRequested: String? = null

    @Option(names = ["--local-configuration", "--local-config", "-lc"], description = ["Set custom location of local configuration file"])
    var localConfigurationFile = LOCAL_CONFIGURATION_FILE
    lateinit var localConfigurationPath: Path

    @Option(names = ["--local-configuration-mode", "--local-config-mode", "-lcm"], description = [
        "Supported local configuration modes:",
        "auto - process and override main configuration file",
        "none - disable automatic updates of configuration file"
    ])
    var localConfigurationMode = LocalConfigurationMode.AUTO

    @Option(names = ["--shared-configuration", "--shared-config", "-sc"], description = ["Set custom location of shared configuration file"])
    var sharedConfigurationFile: String? = null
    var sharedConfigurationPath: Path? = null

    @Option(names = ["--hostname", "-h"], description = ["Override hostname from configuration"])
    var hostname = ""

    @Option(names = ["--port", "-p"], description = ["Override port from configuration"])
    var port = -1

    @Option(names = ["--token", "-t"], description = ["Create temporary token with the given credentials in name:secret format", "Created token has all permissions"])
    var tokenEntries = arrayOf<String>()
    lateinit var tokens: Collection<CreateAccessTokenRequest>

    @Option(names = ["--test-env", "--debug", "-d"], description = ["Enable test mode"])
    var testEnv = false

    override fun run() {
        this.workingDirectory = Paths.get(workingDirectoryName)

        this.localConfigurationPath = localConfigurationFile.let { workingDirectory.resolve(it.ifEmpty { LOCAL_CONFIGURATION_FILE }) }
        this.sharedConfigurationPath = sharedConfigurationFile?.let { workingDirectory.resolve(it.ifEmpty { SHARED_CONFIGURATION_FILE }) }

        this.tokens = tokenEntries
            .map { it.split(":", limit = 2) }
            .map { (name, secret) -> CreateAccessTokenRequest(TEMPORARY, name, secret = secret) }
    }

    fun applyLoadedConfiguration(localConfiguration: LocalConfiguration) {
        if (hostname.isEmpty()) {
            this.hostname = localConfiguration.hostname.get()
        }

        if (port == -1) {
            this.port = localConfiguration.port.get()
        }
    }

}