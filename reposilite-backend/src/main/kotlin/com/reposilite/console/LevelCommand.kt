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
package com.reposilite.console

import com.reposilite.ReposiliteJournalist
import com.reposilite.console.Status.FAILED
import com.reposilite.console.Status.SUCCEEDED
import com.reposilite.journalist.Channel
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "level", description = ["Change current level of visible logging"])
internal class LevelCommand(private val journalist: ReposiliteJournalist) : ReposiliteCommand {

    @Parameters(index = "0", paramLabel = "<level>", description = ["the new threshold"], defaultValue = "info")
    private lateinit var level: String

    override fun execute(output: MutableList<String>): Status {
        val channel = Channel.of(level).orElseGet { null }

        if (channel == null) {
            output.add("The new logging level has been set to $level")
            return FAILED
        }

        journalist.setVisibleThreshold(channel)
        output.add("The new logging level has been set to $level")
        return SUCCEEDED
    }

}