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
package org.panda_lang.reposilite

import org.panda_lang.reposilite.ReposiliteFactory.createReposilite
import org.panda_lang.reposilite.ReposiliteConstants
import kotlin.jvm.JvmStatic
import org.panda_lang.reposilite.ReposiliteLauncher
import org.panda_lang.reposilite.Reposilite
import org.panda_lang.reposilite.utils.RunUtils
import org.panda_lang.reposilite.ReposiliteFactory
import org.panda_lang.reposilite.ReposiliteLauncher.Companion
import org.panda_lang.utilities.commons.function.ThrowingRunnable
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.nio.file.Paths
import java.util.*

@Command(name = "reposilite", version = ["Reposilite " + ReposiliteConstants.VERSION])
class ReposiliteLauncher {

    @Option(names = ["--help", "-H"], usageHelp = true, description = ["display help message"])
    private val usageHelpRequested = false

    @Option(names = ["--version", "-V"], versionHelp = true, description = ["display current version of reposilite"])
    private val versionInfoRequested = false

    @Option(names = ["--test-env", "-te"], description = ["enable test mode"])
    private val testEnv = false

    @Option(names = ["--working-directory", "-wd"], description = ["set custom working directory of application instance"])
    private val workingDirectory: String? = null

    @Option(names = ["--config", "-cfg"], description = ["set custom location of configuration file"])
    private val configurationFile: String? = null

    companion object {
        fun create(vararg args: String?): Optional<Reposilite> {
            val launcher = CommandLine.populateCommand(ReposiliteLauncher(), *args)

            if (launcher.usageHelpRequested) {
                CommandLine.usage(launcher, System.out)
                return Optional.empty()
            }

            if (launcher.versionInfoRequested) {
                println("Reposilite " + ReposiliteConstants.VERSION)
                return Optional.empty()
            }

            return Optional.of(create(launcher.workingDirectory, launcher.configurationFile, false, launcher.testEnv))
        }

        fun create(workingDirectoryString: String?, configurationFileName: String?, servlet: Boolean, testEnv: Boolean): Reposilite {
            var workingDirectory = Paths.get("")

            if (workingDirectoryString != null && !workingDirectoryString.isEmpty()) {
                workingDirectory = Paths.get(workingDirectoryString)
            }

            val configurationFile = workingDirectory.resolve(
                if (configurationFileName == null || configurationFileName.isEmpty()) ReposiliteConstants.CONFIGURATION_FILE_NAME else configurationFileName
            )

            val reposiliteFactory = ReposiliteFactory()
            return reposiliteFactory.createReposilite(configurationFile, workingDirectory, testEnv)
        }
    }
}

fun main(args: Array<String>) {
    ReposiliteLauncher.create(*args).ifPresent { reposilite: Reposilite ->
        RunUtils.ofChecked(reposilite.getFailureService(), ThrowingRunnable { reposilite.launch() }).run()
    }
}