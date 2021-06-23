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
package org.panda_lang.reposilite

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.nio.file.Paths

@Command(name = "reposilite", version = ["Reposilite " + ReposiliteConstants.VERSION])
class ReposiliteLauncher {

    @Option(names = ["--help", "-H"], usageHelp = true, description = ["display help message"])
    private var usageHelpRequested = false

    @Option(names = ["--version", "-V"], versionHelp = true, description = ["display current version of reposilite"])
    private var versionInfoRequested = false

    @Option(names = ["--test-env", "-te"], description = ["enable test mode"])
    private var testEnv = false

    @Option(names = ["--working-directory", "-wd"], description = ["set custom working directory of application instance"])
    private var workingDirectory: String? = null

    @Option(names = ["--config", "-cfg"], description = ["set custom location of configuration file"])
    private var configurationFile: String? = null

    companion object {

        fun create(vararg args: String): Reposilite? {
            val launcher = CommandLine.populateCommand(ReposiliteLauncher(), *args)

            if (launcher.usageHelpRequested) {
                CommandLine.usage(launcher, System.out)
                return null
            }

            if (launcher.versionInfoRequested) {
                println("Reposilite " + ReposiliteConstants.VERSION)
                return null
            }

            return create(launcher.workingDirectory, launcher.configurationFile, launcher.testEnv)
        }

        fun create(workingDirectoryString: String?, configurationFileName: String?, testEnv: Boolean): Reposilite {
            var workingDirectory = Paths.get("")

            if (workingDirectoryString != null && workingDirectoryString.isNotEmpty()) {
                workingDirectory = Paths.get(workingDirectoryString)
            }

            val configurationFile = workingDirectory.resolve(
                if (configurationFileName == null || configurationFileName.isEmpty()) ReposiliteConstants.CONFIGURATION_FILE_NAME
                else configurationFileName
            )

            return ReposiliteFactory.createReposilite(configurationFile, workingDirectory, testEnv)
        }
    }

}

fun main(args: Array<String>) {
    ReposiliteLauncher.create(*args)?.launch()
}